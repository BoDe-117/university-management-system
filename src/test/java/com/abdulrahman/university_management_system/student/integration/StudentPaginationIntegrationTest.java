package com.abdulrahman.university_management_system.student.integration;

import com.abdulrahman.university_management_system.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class StudentPaginationIntegrationTest {

    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // This key is only for tests.
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

    private String adminToken;
    private UUID firstProgramId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("DELETE FROM enrollments");
        jdbcTemplate.update("DELETE FROM students");
        jdbcTemplate.update("DELETE FROM courses");
        jdbcTemplate.update("DELETE FROM programs");
        jdbcTemplate.update("DELETE FROM departments");

        UUID adminId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        firstProgramId = UUID.randomUUID();
        UUID secondProgramId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO users (id, username, password_hash, role) VALUES (?, ?, ?, ?)",
                adminId, "admin", "test-hash", "ADMIN"
        );

        jdbcTemplate.update(
                "INSERT INTO departments (id, code, name) VALUES (?, ?, ?)",
                departmentId, "CS", "Computer Science"
        );

        jdbcTemplate.update(
                "INSERT INTO programs (id, code, name, department_id) VALUES (?, ?, ?, ?)",
                firstProgramId, "PROG-A", "Program A", departmentId
        );

        jdbcTemplate.update(
                "INSERT INTO programs (id, code, name, department_id) VALUES (?, ?, ?, ?)",
                secondProgramId, "PROG-B", "Program B", departmentId
        );

        jdbcTemplate.update(
                """
                INSERT INTO students
                    (student_id, first_name, last_name, email, program_id)
                VALUES (?, ?, ?, ?, ?)
                """,
                "STU-001", "Ahmed", "Ali", "ahmed@example.com", firstProgramId
        );

        jdbcTemplate.update(
                """
                INSERT INTO students
                    (student_id, first_name, last_name, email, program_id)
                VALUES (?, ?, ?, ?, ?)
                """,
                "STU-002", "Sara", "Ahmed", "sara@example.com", firstProgramId
        );

        jdbcTemplate.update(
                """
                INSERT INTO students
                    (student_id, first_name, last_name, email, program_id)
                VALUES (?, ?, ?, ?, ?)
                """,
                "STU-003", "Omar", "Ali", "omar@example.com", secondProgramId
        );

        adminToken = jwtService.generateToken(adminId);
    }

    @Test
    void listStudents_returnsRequestedPage() throws Exception {
        mockMvc.perform(get("/api/v1/students")
                        .header("Authorization", bearer(adminToken))
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "firstName,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].firstName").value("Ahmed"))
                .andExpect(jsonPath("$.content[1].firstName").value("Omar"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void listStudents_filtersByProgramAndPagesResults() throws Exception {
        mockMvc.perform(get("/api/v1/students")
                        .header("Authorization", bearer(adminToken))
                        .param("programId", firstProgramId.toString())
                        .param("page", "1")
                        .param("size", "1")
                        .param("sort", "firstName,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Sara"))
                .andExpect(jsonPath("$.content[0].programId")
                        .value(firstProgramId.toString()))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void listStudents_returns404ForMissingProgram() throws Exception {
        mockMvc.perform(get("/api/v1/students")
                        .header("Authorization", bearer(adminToken))
                        .param("programId", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listStudents_rejectsPageSizeOver100() throws Exception {
        mockMvc.perform(get("/api/v1/students")
                        .header("Authorization", bearer(adminToken))
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listStudents_rejectsUnsupportedSortField() throws Exception {
        mockMvc.perform(get("/api/v1/students")
                        .header("Authorization", bearer(adminToken))
                        .param("sort", "unknown,asc"))
                .andExpect(status().isBadRequest());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
