package com.abdulrahman.university_management_system.security.integration;

import com.abdulrahman.university_management_system.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UserCreationSecurityIntegrationTest {

    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Test-only key: never use this value for a real deployment.
        registry.add(
                "JWT_SECRET",
                () -> "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
        );
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID studentId;
    private String adminToken;
    private String staffToken;
    private String studentToken;

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

        UUID adminId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID studentUserId = UUID.randomUUID();

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
                "INSERT INTO users (id, username, password_hash, role) VALUES (?, ?, ?, ?)",
                adminId, "admin", "test-hash", "ADMIN"
        );

        jdbcTemplate.update(
                "INSERT INTO users (id, username, password_hash, role) VALUES (?, ?, ?, ?)",
                staffId, "staff", "test-hash", "STAFF"
        );

        jdbcTemplate.update(
                "INSERT INTO users (id, username, password_hash, role, student_id) VALUES (?, ?, ?, ?, ?)",
                studentUserId, "sara", "test-hash", "STUDENT", studentId
        );

        adminToken = jwtService.generateToken(adminId);
        staffToken = jwtService.generateToken(staffId);
        studentToken = jwtService.generateToken(studentUserId);
    }

    @Test
    void admin_canCreateStaffAccountWithoutReturningPassword() throws Exception {
        String password = "ExamplePassword123!";

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "new-staff",
                                    "password": "ExamplePassword123!",
                                    "role": "STAFF"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new-staff"))
                .andExpect(jsonPath("$.role").value("STAFF"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        String storedHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE username = ?",
                String.class,
                "new-staff"
        );

        assertThat(storedHash).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, storedHash)).isTrue();
    }

    @Test
    void staff_cannotCreateAccount() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", bearer(staffToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(staffRequest()))
                .andExpect(status().isForbidden());
    }

    @Test
    void student_cannotCreateAccount() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(staffRequest()))
                .andExpect(status().isForbidden());
    }

    @Test
    void requestWithoutToken_cannotCreateAccount() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(staffRequest()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void admin_cannotCreateSecondAccountForSameStudent() throws Exception {
        String request = """
                {
                    "username": "another-student-account",
                    "password": "ExamplePassword123!",
                    "role": "STUDENT",
                    "studentId": "%s"
                }
                """.formatted(studentId);

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict());
    }

    @Test
    void admin_cannotCreateAccountWithShortPassword() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "new-staff",
                                    "password": "short",
                                    "role": "STAFF"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String staffRequest() {
        return """
                {
                    "username": "new-staff",
                    "password": "ExamplePassword123!",
                    "role": "STAFF"
                }
                """;
    }
}
