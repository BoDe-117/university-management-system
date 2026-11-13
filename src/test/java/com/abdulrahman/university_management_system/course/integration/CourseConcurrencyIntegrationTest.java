package com.abdulrahman.university_management_system.course.integration;

import com.abdulrahman.university_management_system.course.dto.CourseResponse;
import com.abdulrahman.university_management_system.course.dto.CreateCourseRequest;
import com.abdulrahman.university_management_system.course.exception.CourseCodeAlreadyExistsException;
import com.abdulrahman.university_management_system.course.repository.CourseRepository;
import com.abdulrahman.university_management_system.course.service.CourseService;
import com.abdulrahman.university_management_system.department.dto.CreateDepartmentRequest;
import com.abdulrahman.university_management_system.department.dto.DepartmentResponse;
import com.abdulrahman.university_management_system.department.service.DepartmentService;
import com.abdulrahman.university_management_system.program.dto.CreateProgramRequest;
import com.abdulrahman.university_management_system.program.dto.ProgramResponse;
import com.abdulrahman.university_management_system.program.service.ProgramService;
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
class CourseConcurrencyIntegrationTest {

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
    private DepartmentService departmentService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private CourseService courseService;

    @MockitoSpyBean
    private CourseRepository courseRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void concurrentCreatesWithSameCodeProduceOneSuccessAndOneConflict()
            throws Exception {
        DepartmentResponse department = departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computer Science")
        );

        ProgramResponse program = programService.createProgram(
                new CreateProgramRequest(
                        "BSC-CS",
                        "Computer Science",
                        department.id()
                )
        );

        CyclicBarrier bothChecksCompleted = new CyclicBarrier(2);

        doAnswer(invocation -> {
            Boolean exists = jdbcTemplate.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM courses WHERE code = ?)",
                    Boolean.class,
                    "RACE-COURSE"
            );

            assertThat(exists).isFalse();

            bothChecksCompleted.await(10, TimeUnit.SECONDS);

            return exists;
        }).when(courseRepository).existsByCode("RACE-COURSE");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch finished = new CountDownLatch(2);

        List<CourseResponse> successes = new CopyOnWriteArrayList<>();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        try {
            for (int i = 0; i < 2; i++) {
                executor.submit(() -> {
                    try {
                        CourseResponse response = courseService.createCourse(
                                new CreateCourseRequest(
                                        "RACE-COURSE",
                                        "Concurrency Test Course",
                                        3,
                                        program.id()
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
                    .as("Both course creation requests should finish")
                    .isTrue();

            assertThat(successes).hasSize(1);
            assertThat(failures).hasSize(1);
            assertThat(failures.getFirst())
                    .isInstanceOf(CourseCodeAlreadyExistsException.class);

            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM courses WHERE code = ?",
                    Integer.class,
                    "RACE-COURSE"
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
