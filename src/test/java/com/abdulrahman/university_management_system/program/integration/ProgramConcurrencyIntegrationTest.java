package com.abdulrahman.university_management_system.program.integration;

import com.abdulrahman.university_management_system.department.dto.CreateDepartmentRequest;
import com.abdulrahman.university_management_system.department.dto.DepartmentResponse;
import com.abdulrahman.university_management_system.department.service.DepartmentService;
import com.abdulrahman.university_management_system.program.dto.CreateProgramRequest;
import com.abdulrahman.university_management_system.program.dto.ProgramResponse;
import com.abdulrahman.university_management_system.program.exception.ProgramCodeAlreadyExistsException;
import com.abdulrahman.university_management_system.program.repository.ProgramRepository;
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
class ProgramConcurrencyIntegrationTest {

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
    private ProgramService programService;

    @Autowired
    private DepartmentService departmentService;

    @MockitoSpyBean
    private ProgramRepository programRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void concurrentCreatesWithSameCodeProduceOneSuccessAndOneConflict()
            throws Exception {
        DepartmentResponse department = departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computer Science")
        );

        CyclicBarrier bothChecksCompleted = new CyclicBarrier(2);

        doAnswer(invocation -> {
            Boolean exists = jdbcTemplate.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM programs WHERE code = ?)",
                    Boolean.class,
                    "RACE-PROGRAM"
            );

            assertThat(exists).isFalse();

            bothChecksCompleted.await(10, TimeUnit.SECONDS);

            return exists;
        }).when(programRepository).existsByCode("RACE-PROGRAM");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch finished = new CountDownLatch(2);

        List<ProgramResponse> successes = new CopyOnWriteArrayList<>();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        try {
            for (int i = 0; i < 2; i++) {
                executor.submit(() -> {
                    try {
                        ProgramResponse response = programService.createProgram(
                                new CreateProgramRequest(
                                        "RACE-PROGRAM",
                                        "Concurrency Test Program",
                                        department.id()
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
                    .as("Both program creation requests should finish")
                    .isTrue();

            assertThat(successes).hasSize(1);
            assertThat(failures).hasSize(1);
            assertThat(failures.getFirst())
                    .isInstanceOf(ProgramCodeAlreadyExistsException.class);

            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM programs WHERE code = ?",
                    Integer.class,
                    "RACE-PROGRAM"
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
