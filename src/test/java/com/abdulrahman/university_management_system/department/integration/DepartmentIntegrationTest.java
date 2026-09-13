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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class DepartmentIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private DepartmentRepository departmentRepository;

    @BeforeEach
    void cleanDatabase() {
        departmentRepository.deleteAll();
    }

    @Test
    void createAndRetrieveDepartment() {
        CreateDepartmentRequest request = new CreateDepartmentRequest("CS", "Computer Science");

        DepartmentResponse created = departmentService.createDepartment(request);

        assertThat(created.id()).isNotNull();
        assertThat(created.code()).isEqualTo("CS");
        assertThat(created.name()).isEqualTo("Computer Science");
        assertThat(created.createdAt()).isNotNull();
        assertThat(created.updatedAt()).isNotNull();

        DepartmentResponse retrieved = departmentService.getDepartmentById(created.id());

        assertThat(retrieved.id()).isEqualTo(created.id());
        assertThat(retrieved.code()).isEqualTo("CS");
    }

    @Test
    void duplicateCodeIsRejected() {
        departmentService.createDepartment(new CreateDepartmentRequest("CS", "Computer Science"));

        assertThatThrownBy(() ->
                departmentService.createDepartment(new CreateDepartmentRequest("CS", "Different Name")))
                .isInstanceOf(DepartmentCodeAlreadyExistsException.class);

        assertThat(departmentRepository.count()).isEqualTo(1);
    }

    @Test
    void migrationCreatesCompatibleSchema() {
        DepartmentResponse created = departmentService.createDepartment(
                new CreateDepartmentRequest("SE", "Software Engineering"));

        DepartmentResponse fetched = departmentService.getDepartmentById(created.id());

        assertThat(fetched.id()).isEqualTo(created.id());
        assertThat(fetched.code()).isEqualTo("SE");
        assertThat(fetched.name()).isEqualTo("Software Engineering");
        assertThat(fetched.createdAt()).isNotNull();
        assertThat(fetched.updatedAt()).isNotNull();
    }

    @Test
    void concurrentDuplicateCreatesProduceOneSuccessAndOneConflict() throws Exception {
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        List<DepartmentResponse> successes = new CopyOnWriteArrayList<>();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    DepartmentResponse response = departmentService.createDepartment(
                            new CreateDepartmentRequest("RACE", "Race Test"));
                    successes.add(response);
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertThat(successes).hasSize(1);
        assertThat(failures).hasSize(1);
        assertThat(failures.getFirst()).isInstanceOf(DepartmentCodeAlreadyExistsException.class);
        assertThat(departmentRepository.count()).isEqualTo(1);
    }
}
