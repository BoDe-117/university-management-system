package com.abdulrahman.university_management_system.department.integration;

import com.abdulrahman.university_management_system.department.dto.CreateDepartmentRequest;
import com.abdulrahman.university_management_system.department.dto.DepartmentResponse;
import com.abdulrahman.university_management_system.department.exception.DepartmentCodeAlreadyExistsException;
import com.abdulrahman.university_management_system.department.repository.DepartmentRepository;
import com.abdulrahman.university_management_system.department.service.DepartmentService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers
class DepartmentIntegrationTest {

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

    @MockitoSpyBean
    private DepartmentRepository departmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        departmentRepository.deleteAll();
    }

    @Test
    void createAndRetrieveDepartment() {
        CreateDepartmentRequest request =
                new CreateDepartmentRequest("CS", "Computer Science");

        DepartmentResponse created =
                departmentService.createDepartment(request);

        assertThat(created.id()).isNotNull();
        assertThat(created.code()).isEqualTo("CS");
        assertThat(created.name()).isEqualTo("Computer Science");
        assertThat(created.createdAt()).isNotNull();
        assertThat(created.updatedAt()).isNotNull();

        DepartmentResponse retrieved =
                departmentService.getDepartmentById(created.id());

        assertThat(retrieved.id()).isEqualTo(created.id());
        assertThat(retrieved.code()).isEqualTo("CS");
    }

    @Test
    void duplicateCodeIsRejected() {
        departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computer Science")
        );

        assertThatThrownBy(() ->
                departmentService.createDepartment(
                        new CreateDepartmentRequest("CS", "Different Name")
                ))
                .isInstanceOf(DepartmentCodeAlreadyExistsException.class);

        assertThat(departmentRepository.count()).isEqualTo(1);
    }

    @Test
    void migrationCreatesCompatibleSchema() {
        DepartmentResponse created =
                departmentService.createDepartment(
                        new CreateDepartmentRequest(
                                "SE", "Software Engineering"
                        )
                );

        DepartmentResponse fetched =
                departmentService.getDepartmentById(created.id());

        assertThat(fetched.id()).isEqualTo(created.id());
        assertThat(fetched.code()).isEqualTo("SE");
        assertThat(fetched.name()).isEqualTo("Software Engineering");
        assertThat(fetched.createdAt()).isNotNull();
        assertThat(fetched.updatedAt()).isNotNull();
    }

    @Test
    void concurrentDuplicateCreatesProduceOneSuccessAndOneConflict()
            throws Exception {

        CyclicBarrier bothChecksCompleted = new CyclicBarrier(2);

        // Perform a real database check, then coordinate both requests
        // so neither can insert until both have confirmed the code is absent.
        doAnswer(invocation -> {
            Boolean exists = jdbcTemplate.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM departments WHERE code = ?)",
                    Boolean.class,
                    "RACE"
            );

            assertThat(exists).isFalse();

            bothChecksCompleted.await(10, TimeUnit.SECONDS);

            return exists;
        }).when(departmentRepository).existsByCode("RACE");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch doneLatch = new CountDownLatch(2);

        List<DepartmentResponse> successes = new CopyOnWriteArrayList<>();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        try {
            for (int i = 0; i < 2; i++) {
                executor.submit(() -> {
                    try {
                        DepartmentResponse response =
                                departmentService.createDepartment(
                                        new CreateDepartmentRequest(
                                                "RACE", "Race Test"
                                        )
                                );

                        successes.add(response);
                    } catch (Throwable failure) {
                        // Bring worker exceptions and assertion failures
                        // back to the test thread for verification.
                        failures.add(failure);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            assertThat(doneLatch.await(30, TimeUnit.SECONDS))
                    .as("Both requests should finish within 30 seconds")
                    .isTrue();

            assertThat(successes).hasSize(1);
            assertThat(failures).hasSize(1);
            assertThat(failures.getFirst())
                    .isInstanceOf(DepartmentCodeAlreadyExistsException.class);

            verify(departmentRepository, times(2)).existsByCode("RACE");
            verify(departmentRepository, times(2)).saveAndFlush(any());

            assertThat(departmentRepository.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();

            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS))
                    .as("Worker threads should terminate after cleanup")
                    .isTrue();
        }
    }
}
