package com.abdulrahman.university_management_system.student.integration;

import com.abdulrahman.university_management_system.department.dto.CreateDepartmentRequest;
import com.abdulrahman.university_management_system.department.dto.DepartmentResponse;
import com.abdulrahman.university_management_system.department.service.DepartmentService;
import com.abdulrahman.university_management_system.program.dto.CreateProgramRequest;
import com.abdulrahman.university_management_system.program.dto.ProgramResponse;
import com.abdulrahman.university_management_system.program.exception.ProgramNotFoundException;
import com.abdulrahman.university_management_system.program.service.ProgramService;
import com.abdulrahman.university_management_system.student.dto.CreateStudentRequest;
import com.abdulrahman.university_management_system.student.dto.StudentResponse;
import com.abdulrahman.university_management_system.student.exception.StudentEmailAlreadyExistsException;
import com.abdulrahman.university_management_system.student.exception.StudentIdAlreadyExistsException;
import com.abdulrahman.university_management_system.student.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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
class StudentIntegrationTest {

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
    private StudentService studentService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM students");
        jdbcTemplate.update("DELETE FROM courses");
        jdbcTemplate.update("DELETE FROM programs");
        jdbcTemplate.update("DELETE FROM departments");
    }

    @Test
    void createAndRetrieveStudent_withProgram() {
        ProgramResponse program = createProgram("BSC-CS");

        StudentResponse created = studentService.createStudent(
                new CreateStudentRequest(
                        "STU-001",
                        "Sara",
                        "Ahmed",
                        "sara@example.com",
                        program.id()
                )
        );

        StudentResponse retrieved = studentService.getStudentById(created.id());

        assertThat(created.id()).isNotNull();
        assertThat(retrieved.studentId()).isEqualTo("STU-001");
        assertThat(retrieved.firstName()).isEqualTo("Sara");
        assertThat(retrieved.lastName()).isEqualTo("Ahmed");
        assertThat(retrieved.email()).isEqualTo("sara@example.com");
        assertThat(retrieved.programId()).isEqualTo(program.id());
        assertThat(retrieved.programCode()).isEqualTo("BSC-CS");
        assertThat(retrieved.createdAt()).isNotNull();
        assertThat(retrieved.updatedAt()).isNotNull();
    }

    @Test
    void createStudent_rejectsDuplicateStudentId() {
        ProgramResponse program = createProgram("BSC-CS");

        studentService.createStudent(
                studentRequest("STU-001", "first@example.com", program.id())
        );

        assertThatThrownBy(() -> studentService.createStudent(
                studentRequest("STU-001", "second@example.com", program.id())
        )).isInstanceOf(StudentIdAlreadyExistsException.class);
    }

    @Test
    void createStudent_rejectsDuplicateEmail() {
        ProgramResponse program = createProgram("BSC-CS");

        studentService.createStudent(
                studentRequest("STU-001", "sara@example.com", program.id())
        );

        assertThatThrownBy(() -> studentService.createStudent(
                studentRequest("STU-002", "sara@example.com", program.id())
        )).isInstanceOf(StudentEmailAlreadyExistsException.class);
    }

    @Test
    void createStudent_rejectsMissingProgram() {
        UUID missingProgramId = UUID.randomUUID();

        assertThatThrownBy(() -> studentService.createStudent(
                studentRequest("STU-001", "sara@example.com", missingProgramId)
        )).isInstanceOf(ProgramNotFoundException.class);
    }

    @Test
    void getStudentsByProgram_returnsOnlyMatchingStudents() {
        DepartmentResponse department = departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computer Science")
        );

        ProgramResponse first = programService.createProgram(
                new CreateProgramRequest("BSC-CS", "Computer Science", department.id())
        );

        ProgramResponse second = programService.createProgram(
                new CreateProgramRequest("BSC-SE", "Software Engineering", department.id())
        );

        studentService.createStudent(
                studentRequest("STU-001", "first@example.com", first.id())
        );

        studentService.createStudent(
                studentRequest("STU-002", "second@example.com", second.id())
        );

        List<StudentResponse> students =
                studentService.getStudentsByProgramId(first.id());

        assertThat(students).hasSize(1);
        assertThat(students.getFirst().studentId()).isEqualTo("STU-001");
        assertThat(students.getFirst().programId()).isEqualTo(first.id());
    }

    private ProgramResponse createProgram(String code) {
        DepartmentResponse department = departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computer Science")
        );

        return programService.createProgram(
                new CreateProgramRequest(code, "Computer Science", department.id())
        );
    }

    private CreateStudentRequest studentRequest(
            String studentId,
            String email,
            UUID programId
    ) {
        return new CreateStudentRequest(
                studentId,
                "Sara",
                "Ahmed",
                email,
                programId
        );
    }
}
