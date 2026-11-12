package com.abdulrahman.university_management_system.program.integration;

import com.abdulrahman.university_management_system.department.dto.CreateDepartmentRequest;
import com.abdulrahman.university_management_system.department.dto.DepartmentResponse;
import com.abdulrahman.university_management_system.department.repository.DepartmentRepository;
import com.abdulrahman.university_management_system.department.service.DepartmentService;
import com.abdulrahman.university_management_system.program.dto.CreateProgramRequest;
import com.abdulrahman.university_management_system.program.dto.ProgramResponse;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class ProgramDeleteIntegrationTest {

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
    void deleteProgram_removesProgramFromDatabase() {
        DepartmentResponse department = departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computer Science")
        );

        ProgramResponse created = programService.createProgram(
                new CreateProgramRequest(
                        "BSC-CS",
                        "Computer Science",
                        department.id()
                )
        );

        programService.deleteProgram(created.id());

        assertThat(programRepository.existsById(created.id())).isFalse();

        assertThatThrownBy(() -> programService.getProgramById(created.id()))
                .isInstanceOf(ProgramNotFoundException.class);

        assertThat(departmentRepository.existsById(department.id())).isTrue();
    }

    @Test
    void deleteProgram_rejectsNonexistentProgram() {
        UUID missingId = UUID.randomUUID();

        assertThatThrownBy(() -> programService.deleteProgram(missingId))
                .isInstanceOf(ProgramNotFoundException.class);
    }

    @Test
    void deleteProgram_allowsCodeToBeReused() {
        DepartmentResponse department = departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computer Science")
        );

        ProgramResponse created = programService.createProgram(
                new CreateProgramRequest(
                        "BSC-CS",
                        "Computer Science",
                        department.id()
                )
        );

        programService.deleteProgram(created.id());

        ProgramResponse replacement = programService.createProgram(
                new CreateProgramRequest(
                        "BSC-CS",
                        "Computing",
                        department.id()
                )
        );

        assertThat(replacement.code()).isEqualTo("BSC-CS");
        assertThat(replacement.id()).isNotEqualTo(created.id());
    }
}
