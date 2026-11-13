package com.abdulrahman.university_management_system.student.integration;

import com.abdulrahman.university_management_system.department.dto.CreateDepartmentRequest;
import com.abdulrahman.university_management_system.department.dto.DepartmentResponse;
import com.abdulrahman.university_management_system.department.service.DepartmentService;
import com.abdulrahman.university_management_system.program.dto.CreateProgramRequest;
import com.abdulrahman.university_management_system.program.dto.ProgramResponse;
import com.abdulrahman.university_management_system.program.service.ProgramService;
import com.abdulrahman.university_management_system.student.dto.CreateStudentRequest;
import com.abdulrahman.university_management_system.student.dto.StudentResponse;
import com.abdulrahman.university_management_system.student.dto.UpdateStudentRequest;
import com.abdulrahman.university_management_system.student.exception.StudentEmailAlreadyExistsException;
import com.abdulrahman.university_management_system.student.exception.StudentIdAlreadyExistsException;
import com.abdulrahman.university_management_system.student.exception.StudentNotFoundException;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class StudentUpdateDeleteIntegrationTest {

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
    void updateStudent_savesChangesAndMovesToAnotherProgram() {
        DepartmentResponse department = createDepartment();
        ProgramResponse first = createProgram("BSC-CS", department.id());
        ProgramResponse second = createProgram("BSC-SE", department.id());

        StudentResponse created = studentService.createStudent(
                new CreateStudentRequest(
                        "STU-001", "Sara", "Ahmed",
                        "sara@example.com", first.id()
                )
        );

        studentService.updateStudent(
                created.id(),
                new UpdateStudentRequest(
                        "STU-002", "Sara", "Ali",
                        "sara.ali@example.com", second.id()
                )
        );

        StudentResponse retrieved = studentService.getStudentById(created.id());

        assertThat(retrieved.studentId()).isEqualTo("STU-002");
        assertThat(retrieved.lastName()).isEqualTo("Ali");
        assertThat(retrieved.email()).isEqualTo("sara.ali@example.com");
        assertThat(retrieved.programId()).isEqualTo(second.id());
    }

    @Test
    void updateStudent_allowsKeepingSameStudentIdAndEmail() {
        DepartmentResponse department = createDepartment();
        ProgramResponse program = createProgram("BSC-CS", department.id());

        StudentResponse created = studentService.createStudent(
                new CreateStudentRequest(
                        "STU-001", "Sara", "Ahmed",
                        "sara@example.com", program.id()
                )
        );

        studentService.updateStudent(
                created.id(),
                new UpdateStudentRequest(
                        "STU-001", "Sara", "Ali",
                        "sara@example.com", program.id()
                )
        );

        StudentResponse retrieved = studentService.getStudentById(created.id());

        assertThat(retrieved.studentId()).isEqualTo("STU-001");
        assertThat(retrieved.email()).isEqualTo("sara@example.com");
        assertThat(retrieved.lastName()).isEqualTo("Ali");
    }

    @Test
    void updateStudent_rejectsStudentIdUsedByAnotherStudent() {
        DepartmentResponse department = createDepartment();
        ProgramResponse program = createProgram("BSC-CS", department.id());

        studentService.createStudent(
                new CreateStudentRequest(
                        "STU-001", "Sara", "Ahmed",
                        "sara@example.com", program.id()
                )
        );

        StudentResponse other = studentService.createStudent(
                new CreateStudentRequest(
                        "STU-002", "Ali", "Saleh",
                        "ali@example.com", program.id()
                )
        );

        assertThatThrownBy(() -> studentService.updateStudent(
                other.id(),
                new UpdateStudentRequest(
                        "STU-001", "Ali", "Saleh",
                        "ali@example.com", program.id()
                )
        )).isInstanceOf(StudentIdAlreadyExistsException.class);

        assertThat(studentService.getStudentById(other.id()).studentId())
                .isEqualTo("STU-002");
    }

    @Test
    void updateStudent_rejectsEmailUsedByAnotherStudent() {
        DepartmentResponse department = createDepartment();
        ProgramResponse program = createProgram("BSC-CS", department.id());

        studentService.createStudent(
                new CreateStudentRequest(
                        "STU-001", "Sara", "Ahmed",
                        "sara@example.com", program.id()
                )
        );

        StudentResponse other = studentService.createStudent(
                new CreateStudentRequest(
                        "STU-002", "Ali", "Saleh",
                        "ali@example.com", program.id()
                )
        );

        assertThatThrownBy(() -> studentService.updateStudent(
                other.id(),
                new UpdateStudentRequest(
                        "STU-002", "Ali", "Saleh",
                        "sara@example.com", program.id()
                )
        )).isInstanceOf(StudentEmailAlreadyExistsException.class);

        assertThat(studentService.getStudentById(other.id()).email())
                .isEqualTo("ali@example.com");
    }

    @Test
    void deleteStudent_removesStudentAndAllowsValuesToBeReused() {
        DepartmentResponse department = createDepartment();
        ProgramResponse program = createProgram("BSC-CS", department.id());

        StudentResponse created = studentService.createStudent(
                new CreateStudentRequest(
                        "STU-001", "Sara", "Ahmed",
                        "sara@example.com", program.id()
                )
        );

        studentService.deleteStudent(created.id());

        assertThatThrownBy(() -> studentService.getStudentById(created.id()))
                .isInstanceOf(StudentNotFoundException.class);

        StudentResponse replacement = studentService.createStudent(
                new CreateStudentRequest(
                        "STU-001", "Sara", "Ali",
                        "sara@example.com", program.id()
                )
        );

        assertThat(replacement.id()).isNotEqualTo(created.id());
        assertThat(replacement.studentId()).isEqualTo("STU-001");
        assertThat(replacement.email()).isEqualTo("sara@example.com");
    }

    @Test
    void deleteStudent_rejectsMissingStudent() {
        UUID missingId = UUID.randomUUID();

        assertThatThrownBy(() -> studentService.deleteStudent(missingId))
                .isInstanceOf(StudentNotFoundException.class);
    }

    private DepartmentResponse createDepartment() {
        return departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computer Science")
        );
    }

    private ProgramResponse createProgram(String code, UUID departmentId) {
        return programService.createProgram(
                new CreateProgramRequest(code, "Test Program", departmentId)
        );
    }
}
