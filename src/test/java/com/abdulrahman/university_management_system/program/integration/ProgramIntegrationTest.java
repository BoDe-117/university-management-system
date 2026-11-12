package com.abdulrahman.university_management_system.program.integration;

import com.abdulrahman.university_management_system.department.dto.CreateDepartmentRequest;
import com.abdulrahman.university_management_system.department.dto.DepartmentResponse;
import com.abdulrahman.university_management_system.department.exception.DepartmentNotFoundException;
import com.abdulrahman.university_management_system.department.repository.DepartmentRepository;
import com.abdulrahman.university_management_system.department.service.DepartmentService;
import com.abdulrahman.university_management_system.program.dto.CreateProgramRequest;
import com.abdulrahman.university_management_system.program.dto.ProgramResponse;
import com.abdulrahman.university_management_system.program.exception.ProgramCodeAlreadyExistsException;
import com.abdulrahman.university_management_system.program.exception.ProgramNotFoundException;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class ProgramIntegrationTest {

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
    void createAndRetrieveProgram_withItsDepartment() {
        DepartmentResponse department = departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computer Science")
        );

        ProgramResponse created = programService.createProgram(
                new CreateProgramRequest(
                        "BSC-CS",
                        "BSc in Computer Science",
                        department.id()
                )
        );

        ProgramResponse retrieved = programService.getProgramById(created.id());

        assertThat(created.id()).isNotNull();
        assertThat(retrieved.id()).isEqualTo(created.id());
        assertThat(retrieved.code()).isEqualTo("BSC-CS");
        assertThat(retrieved.name()).isEqualTo("BSc in Computer Science");
        assertThat(retrieved.departmentId()).isEqualTo(department.id());
        assertThat(retrieved.departmentCode()).isEqualTo("CS");
        assertThat(retrieved.departmentName()).isEqualTo("Computer Science");
        assertThat(retrieved.createdAt()).isNotNull();
        assertThat(retrieved.updatedAt()).isNotNull();
    }

    @Test
    void createProgram_rejectsDuplicateCode() {
        DepartmentResponse department = departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computer Science")
        );

        programService.createProgram(
                new CreateProgramRequest(
                        "BSC-CS",
                        "BSc in Computer Science",
                        department.id()
                )
        );

        assertThatThrownBy(() -> programService.createProgram(
                new CreateProgramRequest(
                        "BSC-CS",
                        "Another Program",
                        department.id()
                )
        )).isInstanceOf(ProgramCodeAlreadyExistsException.class);

        assertThat(programRepository.count()).isEqualTo(1);
    }

    @Test
    void createProgram_rejectsMissingDepartment() {
        UUID missingDepartmentId = UUID.randomUUID();

        assertThatThrownBy(() -> programService.createProgram(
                new CreateProgramRequest(
                        "BSC-CS",
                        "BSc in Computer Science",
                        missingDepartmentId
                )
        )).isInstanceOf(DepartmentNotFoundException.class);

        assertThat(programRepository.count()).isZero();
    }

    @Test
    void getProgramsByDepartment_returnsOnlyMatchingPrograms() {
        DepartmentResponse cs = departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computer Science")
        );

        DepartmentResponse se = departmentService.createDepartment(
                new CreateDepartmentRequest("SE", "Software Engineering")
        );

        programService.createProgram(
                new CreateProgramRequest("BSC-CS", "Computer Science", cs.id())
        );

        programService.createProgram(
                new CreateProgramRequest("BSC-SE", "Software Engineering", se.id())
        );

        List<ProgramResponse> programs =
                programService.getProgramsByDepartmentId(cs.id());

        assertThat(programs).hasSize(1);
        assertThat(programs.getFirst().code()).isEqualTo("BSC-CS");
        assertThat(programs.getFirst().departmentId()).isEqualTo(cs.id());
    }

    @Test
    void getProgramsByDepartment_rejectsMissingDepartment() {
        UUID missingDepartmentId = UUID.randomUUID();

        assertThatThrownBy(() ->
                programService.getProgramsByDepartmentId(missingDepartmentId)
        ).isInstanceOf(DepartmentNotFoundException.class);
    }

    @Test
    void getProgramById_rejectsMissingProgram() {
        UUID missingProgramId = UUID.randomUUID();

        assertThatThrownBy(() ->
                programService.getProgramById(missingProgramId)
        ).isInstanceOf(ProgramNotFoundException.class);
    }
}
