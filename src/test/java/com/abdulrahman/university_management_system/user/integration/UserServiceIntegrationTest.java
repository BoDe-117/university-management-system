package com.abdulrahman.university_management_system.user.integration;

import com.abdulrahman.university_management_system.user.dto.CreateUserRequest;
import com.abdulrahman.university_management_system.user.dto.UserResponse;
import com.abdulrahman.university_management_system.user.entity.UserRole;
import com.abdulrahman.university_management_system.user.exception.StudentAlreadyHasAccountException;
import com.abdulrahman.university_management_system.user.exception.UsernameAlreadyExistsException;
import com.abdulrahman.university_management_system.user.repository.UserRepository;
import com.abdulrahman.university_management_system.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
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
class UserServiceIntegrationTest {

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
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    }

    @Test
    void createStudentAccount_storesEncodedPasswordAndStudentLink() {
        String password = "ExamplePassword123!";

        UserResponse response = userService.createUser(
                new CreateUserRequest("sara", password, UserRole.STUDENT, studentId)
        );

        String storedHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE id = ?",
                String.class,
                response.id()
        );

        UUID storedStudentId = jdbcTemplate.queryForObject(
                "SELECT student_id FROM users WHERE id = ?",
                UUID.class,
                response.id()
        );

        assertThat(response.id()).isNotNull();
        assertThat(response.role()).isEqualTo(UserRole.STUDENT);
        assertThat(storedStudentId).isEqualTo(studentId);
        assertThat(storedHash).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, storedHash)).isTrue();
    }

    @Test
    void duplicateUsername_isTranslatedFromDatabaseConstraint() {
        userService.createUser(
                new CreateUserRequest(
                        "admin",
                        "ExamplePassword123!",
                        UserRole.ADMIN,
                        null
                )
        );

        doReturn(false).when(userRepository).existsByUsername("admin");

        assertThatThrownBy(() -> userService.createUser(
                new CreateUserRequest(
                        "admin",
                        "AnotherPassword123!",
                        UserRole.STAFF,
                        null
                )
        )).isInstanceOf(UsernameAlreadyExistsException.class);
    }

    @Test
    void duplicateStudentAccount_isTranslatedFromDatabaseConstraint() {
        userService.createUser(
                new CreateUserRequest(
                        "sara",
                        "ExamplePassword123!",
                        UserRole.STUDENT,
                        studentId
                )
        );

        doReturn(false).when(userRepository).existsByStudentId(studentId);

        assertThatThrownBy(() -> userService.createUser(
                new CreateUserRequest(
                        "second-user",
                        "AnotherPassword123!",
                        UserRole.STUDENT,
                        studentId
                )
        )).isInstanceOf(StudentAlreadyHasAccountException.class);
    }
}
