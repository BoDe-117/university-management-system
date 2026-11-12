package com.abdulrahman.university_management_system.program.integration;

import com.abdulrahman.university_management_system.department.dto.CreateDepartmentRequest;
import com.abdulrahman.university_management_system.department.dto.DepartmentResponse;
import com.abdulrahman.university_management_system.department.exception.DepartmentNotFoundException;
import com.abdulrahman.university_management_system.department.repository.DepartmentRepository;
import com.abdulrahman.university_management_system.department.service.DepartmentService;
import com.abdulrahman.university_management_system.program.dto.CreateProgramRequest;
import com.abdulrahman.university_management_system.program.dto.ProgramResponse;
import com.abdulrahman.university_management_system.program.dto.UpdateProgramRequest;
import com.abdulrahman.university_management_system.program.exception.ProgramCodeAlreadyExistsException;
import com.abdulrahman.university_management_system.program.repository.ProgramRepository;
import com.abdulrahman.university_management_system.program.service.ProgramService;
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
class ProgramUpdateIntegrationTest {

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

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @BeforeEach
    void cleanDatabase() {
        programRepository.deleteAll();
        departmentRepository.deleteAll();
    }

    @Test
    void updateProgram_savesChangesAndMovesToAnotherDepartment() {
        DepartmentResponse cs = departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computer Science")
        );

        DepartmentResponse se = departmentService.createDepartment(
                new CreateDepartmentRequest("SE", "Software Engineering")
        );

        ProgramResponse created = programService.createProgram(
                new CreateProgramRequest("BSC-CS", "Computer Science", cs.id())
        );

        programService.updateProgram(
                created.id(),
                new UpdateProgramRequest(
                        "BSC-SE",
                        "Software Engineering",
                        se.id()
                )
        );

        ProgramResponse retrieved = programService.getProgramById(created.id());

        assertThat(retrieved.code()).isEqualTo("BSC-SE");
        assertThat(retrieved.name()).isEqualTo("Software Engineering");
        assertThat(retrieved.departmentId()).isEqualTo(se.id());
        assertThat(retrieved.departmentCode()).isEqualTo("SE");
    }

    @Test
    void updateProgram_allowsKeepingSameCode() {
        DepartmentResponse department = departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computer Science")
        );

        ProgramResponse created = programService.createProgram(
                new CreateProgramRequest(
                        "BSC-CS", "Computer Science", department.id()
                )
        );

        programService.updateProgram(
                created.id(),
                new UpdateProgramRequest(
                        "BSC-CS", "Computing", department.id()
                )
        );

        ProgramResponse retrieved = programService.getProgramById(created.id());

        assertThat(retrieved.code()).isEqualTo("BSC-CS");
        assertThat(retrieved.name()).isEqualTo("Computing");
    }

    @Test
    void updateProgram_rejectsCodeUsedByAnotherProgram() {
        DepartmentResponse department = departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computer Science")
        );

        programService.createProgram(
                new CreateProgramRequest("BSC-CS", "Computer Science", department.id())
        );

        ProgramResponse other = programService.createProgram(
                new CreateProgramRequest("BSC-IT", "Information Technology", department.id())
        );

        assertThatThrownBy(() -> programService.updateProgram(
                other.id(),
                new UpdateProgramRequest("BSC-CS", "Different Name", department.id())
        )).isInstanceOf(ProgramCodeAlreadyExistsException.class);

        ProgramResponse retrieved = programService.getProgramById(other.id());

        assertThat(retrieved.code()).isEqualTo("BSC-IT");
        assertThat(retrieved.name()).isEqualTo("Information Technology");
    }

    @Test
    void updateProgram_rejectsMissingDepartment() {
        DepartmentResponse department = departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computer Science")
        );

        ProgramResponse created = programService.createProgram(
                new CreateProgramRequest(
                        "BSC-CS", "Computer Science", department.id()
                )
        );

        UUID missingDepartmentId = UUID.randomUUID();

        assertThatThrownBy(() -> programService.updateProgram(
                created.id(),
                new UpdateProgramRequest(
                        "BSC-CS", "Computer Science", missingDepartmentId
                )
        )).isInstanceOf(DepartmentNotFoundException.class);

        ProgramResponse retrieved = programService.getProgramById(created.id());
        assertThat(retrieved.departmentId()).isEqualTo(department.id());
    }
}
