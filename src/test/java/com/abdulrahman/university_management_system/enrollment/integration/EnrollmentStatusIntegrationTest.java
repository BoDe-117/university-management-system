package com.abdulrahman.university_management_system.enrollment.integration;

import com.abdulrahman.university_management_system.enrollment.dto.CreateEnrollmentRequest;
import com.abdulrahman.university_management_system.enrollment.dto.EnrollmentResponse;
import com.abdulrahman.university_management_system.enrollment.entity.EnrollmentStatus;
import com.abdulrahman.university_management_system.enrollment.exception.InvalidEnrollmentStatusTransitionException;
import com.abdulrahman.university_management_system.enrollment.service.EnrollmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class EnrollmentStatusIntegrationTest {

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

    private UUID enrollmentId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM enrollments");
        jdbcTemplate.update("DELETE FROM students");
        jdbcTemplate.update("DELETE FROM courses");
        jdbcTemplate.update("DELETE FROM programs");
        jdbcTemplate.update("DELETE FROM departments");

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

        EnrollmentResponse enrollment = enrollmentService.createEnrollment(
                new CreateEnrollmentRequest(studentId, courseId)
        );

        enrollmentId = enrollment.id();
    }

    @Test
    void dropEnrollment_updatesStatusInDatabase() {
        EnrollmentResponse response =
                enrollmentService.dropEnrollment(enrollmentId);

        EnrollmentResponse retrieved =
                enrollmentService.getEnrollmentById(enrollmentId);

        assertThat(response.status()).isEqualTo(EnrollmentStatus.DROPPED);
        assertThat(retrieved.status()).isEqualTo(EnrollmentStatus.DROPPED);
    }

    @Test
    void completeEnrollment_updatesStatusInDatabase() {
        EnrollmentResponse response =
                enrollmentService.completeEnrollment(enrollmentId);

        EnrollmentResponse retrieved =
                enrollmentService.getEnrollmentById(enrollmentId);

        assertThat(response.status()).isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(retrieved.status()).isEqualTo(EnrollmentStatus.COMPLETED);
    }

    @Test
    void completedEnrollment_cannotBeDropped() {
        enrollmentService.completeEnrollment(enrollmentId);

        assertThatThrownBy(() ->
                enrollmentService.dropEnrollment(enrollmentId)
        ).isInstanceOf(InvalidEnrollmentStatusTransitionException.class);

        assertThat(enrollmentService.getEnrollmentById(enrollmentId).status())
                .isEqualTo(EnrollmentStatus.COMPLETED);
    }

    @Test
    void concurrentDropAndComplete_produceOneSuccessAndOneConflict()
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(2);

        List<EnrollmentResponse> successes = new CopyOnWriteArrayList<>();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        try {
            executor.submit(() -> {
                try {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Start signal timed out");
                    }
                    successes.add(enrollmentService.dropEnrollment(enrollmentId));
                } catch (Throwable failure) {
                    failures.add(failure);
                } finally {
                    finished.countDown();
                }
            });

            executor.submit(() -> {
                try {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Start signal timed out");
                    }
                    successes.add(enrollmentService.completeEnrollment(enrollmentId));
                } catch (Throwable failure) {
                    failures.add(failure);
                } finally {
                    finished.countDown();
                }
            });

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(finished.await(30, TimeUnit.SECONDS)).isTrue();
            assertThat(successes).hasSize(1);
            assertThat(failures).hasSize(1);
            assertThat(failures.getFirst())
                    .isInstanceOf(InvalidEnrollmentStatusTransitionException.class);

            EnrollmentStatus savedStatus =
                    enrollmentService.getEnrollmentById(enrollmentId).status();

            assertThat(savedStatus).isEqualTo(successes.getFirst().status());
            assertThat(savedStatus).isIn(
                    EnrollmentStatus.DROPPED,
                    EnrollmentStatus.COMPLETED
            );
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }
}
