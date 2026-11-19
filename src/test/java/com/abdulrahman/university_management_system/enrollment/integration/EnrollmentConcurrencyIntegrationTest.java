package com.abdulrahman.university_management_system.enrollment.integration;

import com.abdulrahman.university_management_system.enrollment.dto.CreateEnrollmentRequest;
import com.abdulrahman.university_management_system.enrollment.dto.EnrollmentResponse;
import com.abdulrahman.university_management_system.enrollment.exception.EnrollmentAlreadyExistsException;
import com.abdulrahman.university_management_system.enrollment.repository.EnrollmentRepository;
import com.abdulrahman.university_management_system.enrollment.service.EnrollmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@Testcontainers
class EnrollmentConcurrencyIntegrationTest {

    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private EnrollmentRepository enrollmentRepository;

    @Test
    void concurrentCreatesForSameStudentAndCourseProduceOneSuccessAndOneConflict()
            throws Exception {

        UUID departmentId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO departments (id, code, name) VALUES (?, ?, ?)",
                departmentId, "CS", "Computer Science"
        );

        jdbcTemplate.update(
                "INSERT INTO programs (id, code, name, department_id) VALUES (?, ?, ?, ?)",
                programId, "BSC-CS", "Computer Science", departmentId
        );

        jdbcTemplate.update(
                "INSERT INTO students (id, student_id, first_name, last_name, email, program_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                studentId, "STU-001", "Sara", "Ahmed", "sara@example.com", programId
        );

        jdbcTemplate.update(
                "INSERT INTO courses (id, code, name, credits, program_id) VALUES (?, ?, ?, ?, ?)",
                courseId, "CS101", "Introduction to Programming", 3, programId
        );

        CyclicBarrier bothChecksCompleted = new CyclicBarrier(2);

        doAnswer(invocation -> {
            Boolean exists = jdbcTemplate.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM enrollments "
                            + "WHERE student_id = ? AND course_id = ?)",
                    Boolean.class,
                    studentId,
                    courseId
            );

            assertThat(exists).isFalse();

            bothChecksCompleted.await(10, TimeUnit.SECONDS);

            return exists;
        }).when(enrollmentRepository)
                .existsByStudentIdAndCourseId(studentId, courseId);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch finished = new CountDownLatch(2);

        List<EnrollmentResponse> successes = new CopyOnWriteArrayList<>();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        try {
            for (int i = 0; i < 2; i++) {
                executor.submit(() -> {
                    try {
                        EnrollmentResponse response =
                                enrollmentService.createEnrollment(
                                        new CreateEnrollmentRequest(
                                                studentId,
                                                courseId
                                        )
                                );

                        successes.add(response);
                    } catch (Throwable failure) {
                        failures.add(failure);
                    } finally {
                        finished.countDown();
                    }
                });
            }

            assertThat(finished.await(30, TimeUnit.SECONDS))
                    .as("Both enrollment requests should finish")
                    .isTrue();

            assertThat(successes).hasSize(1);
            assertThat(failures).hasSize(1);
            assertThat(failures.getFirst())
                    .isInstanceOf(EnrollmentAlreadyExistsException.class);

            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM enrollments "
                            + "WHERE student_id = ? AND course_id = ?",
                    Integer.class,
                    studentId,
                    courseId
            );

            assertThat(count).isEqualTo(1);
        } finally {
            executor.shutdownNow();

            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS))
                    .as("Worker threads should terminate")
                    .isTrue();
        }
    }
}
