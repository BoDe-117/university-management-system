package com.abdulrahman.university_management_system.department.integration;

import com.abdulrahman.university_management_system.department.dto.CreateDepartmentRequest;
import com.abdulrahman.university_management_system.department.dto.DepartmentResponse;
import com.abdulrahman.university_management_system.department.dto.UpdateDepartmentRequest;
import com.abdulrahman.university_management_system.department.exception.DepartmentCodeAlreadyExistsException;
import com.abdulrahman.university_management_system.department.exception.DepartmentNotFoundException;
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
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@Testcontainers
class DepartmentUpdateIntegrationTest {

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
        jdbcTemplate.update("DELETE FROM programs");
        departmentRepository.deleteAll();
    }

    @Test
    void updateDepartment_savesChangesToDatabase() {
        DepartmentResponse created = departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computer Science")
        );

        DepartmentResponse beforeUpdate =
                departmentService.getDepartmentById(created.id());

        DepartmentResponse updated = departmentService.updateDepartment(
                created.id(),
                new UpdateDepartmentRequest("SE", "Software Engineering")
        );

        DepartmentResponse retrieved =
                departmentService.getDepartmentById(created.id());

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(retrieved.code()).isEqualTo("SE");
        assertThat(retrieved.name()).isEqualTo("Software Engineering");
        assertThat(retrieved.createdAt()).isEqualTo(beforeUpdate.createdAt());
    }

    @Test
    void updateDepartment_allowsKeepingSameCode() {
        DepartmentResponse created = departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computer Science")
        );

        departmentService.updateDepartment(
                created.id(),
                new UpdateDepartmentRequest("CS", "Computing")
        );

        DepartmentResponse retrieved =
                departmentService.getDepartmentById(created.id());

        assertThat(retrieved.code()).isEqualTo("CS");
        assertThat(retrieved.name()).isEqualTo("Computing");
    }

    @Test
    void updateDepartment_rejectsCodeUsedByAnotherDepartment() {
        departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computer Science")
        );

        DepartmentResponse other = departmentService.createDepartment(
                new CreateDepartmentRequest("SE", "Software Engineering")
        );

        assertThatThrownBy(() -> departmentService.updateDepartment(
                other.id(),
                new UpdateDepartmentRequest("CS", "Different Name")
        )).isInstanceOf(DepartmentCodeAlreadyExistsException.class);

        DepartmentResponse retrieved =
                departmentService.getDepartmentById(other.id());

        assertThat(retrieved.code()).isEqualTo("SE");
        assertThat(retrieved.name()).isEqualTo("Software Engineering");
    }

    @Test
    void updateDepartment_rejectsNonexistentDepartment() {
        UUID missingId = UUID.randomUUID();

        assertThatThrownBy(() -> departmentService.updateDepartment(
                missingId,
                new UpdateDepartmentRequest("CS", "Computer Science")
        )).isInstanceOf(DepartmentNotFoundException.class);
    }

    @Test
    void concurrentUpdatesToSameCodeProduceOneSuccessAndOneConflict()
            throws Exception {
        DepartmentResponse first = departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computer Science")
        );

        DepartmentResponse second = departmentService.createDepartment(
                new CreateDepartmentRequest("SE", "Software Engineering")
        );

        CyclicBarrier bothChecksCompleted = new CyclicBarrier(2);

        doAnswer(invocation -> {
            UUID departmentId = invocation.getArgument(1);

            Boolean exists = jdbcTemplate.queryForObject(
                    """
                    SELECT EXISTS (
                        SELECT 1 FROM departments
                        WHERE code = ? AND id <> ?
                    )
                    """,
                    Boolean.class,
                    "NEW",
                    departmentId
            );

            assertThat(exists).isFalse();

            bothChecksCompleted.await(10, TimeUnit.SECONDS);

            return exists;
        }).when(departmentRepository)
                .existsByCodeAndIdNot(eq("NEW"), any(UUID.class));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch finished = new CountDownLatch(2);

        List<DepartmentResponse> successes = new CopyOnWriteArrayList<>();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        try {
            for (DepartmentResponse department : List.of(first, second)) {
                executor.submit(() -> {
                    try {
                        DepartmentResponse response =
                                departmentService.updateDepartment(
                                        department.id(),
                                        new UpdateDepartmentRequest(
                                                "NEW",
                                                "Updated Department"
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
                    .as("Both update requests should finish")
                    .isTrue();

            assertThat(successes).hasSize(1);
            assertThat(failures).hasSize(1);
            assertThat(failures.getFirst())
                    .isInstanceOf(DepartmentCodeAlreadyExistsException.class);

            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM departments WHERE code = ?",
                    Integer.class,
                    "NEW"
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
