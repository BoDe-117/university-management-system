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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Autowired
    private DepartmentRepository departmentRepository;

    @BeforeEach
    void cleanDatabase() {
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
}