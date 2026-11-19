package com.abdulrahman.university_management_system.user.integration;

import com.abdulrahman.university_management_system.student.exception.StudentHasUserAccountException;
import com.abdulrahman.university_management_system.student.service.StudentService;
import com.abdulrahman.university_management_system.user.repository.UserRepository;
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
class StudentAccountDeleteIntegrationTest {

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
    private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private UserRepository userRepository;

    private UUID studentId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("DELETE FROM enrollments");
        jdbcTemplate.update("DELETE FROM students");
        jdbcTemplate.update("DELETE FROM courses");
        jdbcTemplate.update("DELETE FROM programs");
        jdbcTemplate.update("DELETE FROM departments");

        UUID departmentId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();
        studentId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO departments (id, code, name) VALUES (?, ?, ?)",
                departmentId, "CS", "Computer Science"
        );

        jdbcTemplate.update(
                "INSERT INTO programs (id, code, name, department_id) VALUES (?, ?, ?, ?)",
                programId, "BSC-CS", "Computer Science", departmentId
        );

        jdbcTemplate.update(
                "INSERT INTO students (id, student_id, first_name, last_name, email, program_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                studentId, "STU-001", "Sara", "Ahmed", "sara@example.com", programId
        );

        jdbcTemplate.update(
                "INSERT INTO users (username, password_hash, role, student_id) VALUES (?, ?, ?, ?)",
                "sara", "test-hash", "STUDENT", studentId
        );
    }

    @Test
    void deleteStudent_rejectsStudentWithUserAccount() {
        assertThatThrownBy(() -> studentService.deleteStudent(studentId))
                .isInstanceOf(StudentHasUserAccountException.class);

        assertThat(studentExists()).isTrue();
    }

    @Test
    void deleteStudent_translatesDatabaseForeignKeyViolation() {
        doReturn(false).when(userRepository).existsByStudentId(studentId);

        assertThatThrownBy(() -> studentService.deleteStudent(studentId))
                .isInstanceOf(StudentHasUserAccountException.class);

        assertThat(studentExists()).isTrue();
    }

    @Test
    void deleteStudent_succeedsWhenUserAccountIsRemoved() {
        jdbcTemplate.update("DELETE FROM users WHERE student_id = ?", studentId);

        studentService.deleteStudent(studentId);

        assertThat(studentExists()).isFalse();
    }

    private boolean studentExists() {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM students WHERE id = ?)",
                Boolean.class,
                studentId
        );

        return Boolean.TRUE.equals(exists);
    }
}
