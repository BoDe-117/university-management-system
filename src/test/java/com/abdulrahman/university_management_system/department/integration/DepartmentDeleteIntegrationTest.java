package com.abdulrahman.university_management_system.department.integration;

import com.abdulrahman.university_management_system.department.dto.CreateDepartmentRequest;
import com.abdulrahman.university_management_system.department.dto.DepartmentResponse;
import com.abdulrahman.university_management_system.department.exception.DepartmentHasAssociatedProgramsException;
import com.abdulrahman.university_management_system.department.exception.DepartmentNotFoundException;
import com.abdulrahman.university_management_system.department.repository.DepartmentRepository;
import com.abdulrahman.university_management_system.department.service.DepartmentService;
import com.abdulrahman.university_management_system.program.repository.ProgramRepository;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

@SpringBootTest
@Testcontainers
class DepartmentDeleteIntegrationTest {

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

    @MockitoSpyBean
    private ProgramRepository programRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM programs");
        departmentRepository.deleteAll();
    }

    @Test
    void deleteDepartment_removesDepartmentFromDatabase() {
        DepartmentResponse created = departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computer Science")
        );

        departmentService.deleteDepartment(created.id());

        assertThat(departmentRepository.existsById(created.id())).isFalse();

        assertThatThrownBy(() ->
                departmentService.getDepartmentById(created.id())
        ).isInstanceOf(DepartmentNotFoundException.class);
    }

    @Test
    void deleteDepartment_throwsNotFound_whenDepartmentDoesNotExist() {
        UUID missingId = UUID.randomUUID();

        assertThatThrownBy(() -> departmentService.deleteDepartment(missingId))
                .isInstanceOf(DepartmentNotFoundException.class);
    }

    @Test
    void deleteDepartment_rejectsDepartmentWithPrograms() {
        DepartmentResponse created = departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computer Science")
        );

        insertProgram(created.id());

        assertThatThrownBy(() -> departmentService.deleteDepartment(created.id()))
                .isInstanceOf(DepartmentHasAssociatedProgramsException.class);

        assertThat(departmentRepository.existsById(created.id())).isTrue();
        assertThat(programRepository.existsByDepartmentId(created.id())).isTrue();
    }

    @Test
    void deleteDepartment_translatesForeignKeyViolation() {
        DepartmentResponse created = departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computer Science")
        );

        insertProgram(created.id());

        // Simulate a program appearing after the initial existence check.
        doReturn(false).when(programRepository)
                .existsByDepartmentId(created.id());

        assertThatThrownBy(() -> departmentService.deleteDepartment(created.id()))
                .isInstanceOf(DepartmentHasAssociatedProgramsException.class);

        assertThat(departmentRepository.existsById(created.id())).isTrue();

        Integer programCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM programs WHERE department_id = ?",
                Integer.class,
                created.id()
        );

        assertThat(programCount).isEqualTo(1);
    }

    @Test
    void deleteDepartment_allowsCodeToBeReused() {
        DepartmentResponse created = departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computer Science")
        );

        departmentService.deleteDepartment(created.id());

        DepartmentResponse replacement = departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computing")
        );

        assertThat(replacement.code()).isEqualTo("CS");
        assertThat(replacement.id()).isNotEqualTo(created.id());
    }

    private void insertProgram(UUID departmentId) {
        jdbcTemplate.update(
                """
                INSERT INTO programs (id, code, name, department_id)
                VALUES (?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                "BSC-CS",
                "BSc in Computer Science",
                departmentId
        );
    }
}

