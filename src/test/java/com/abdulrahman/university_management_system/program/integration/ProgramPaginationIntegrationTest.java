package com.abdulrahman.university_management_system.program.integration;

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
class ProgramPaginationIntegrationTest {

    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Test-only key.
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
    private UUID firstDepartmentId;
    private UUID secondDepartmentId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("DELETE FROM enrollments");
        jdbcTemplate.update("DELETE FROM students");
        jdbcTemplate.update("DELETE FROM courses");
        jdbcTemplate.update("DELETE FROM programs");
        jdbcTemplate.update("DELETE FROM departments");

        UUID adminId = UUID.randomUUID();
        firstDepartmentId = UUID.randomUUID();
        secondDepartmentId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO users (id, username, password_hash, role) VALUES (?, ?, ?, ?)",
                adminId, "admin", "test-hash", "ADMIN"
        );

        jdbcTemplate.update(
                "INSERT INTO departments (id, code, name) VALUES (?, ?, ?)",
                firstDepartmentId, "CS", "Computer Science"
        );

        jdbcTemplate.update(
                "INSERT INTO departments (id, code, name) VALUES (?, ?, ?)",
                secondDepartmentId, "BUS", "Business"
        );

        jdbcTemplate.update(
                "INSERT INTO programs (code, name, department_id) VALUES (?, ?, ?)",
                "CS-A", "Alpha", firstDepartmentId
        );

        jdbcTemplate.update(
                "INSERT INTO programs (code, name, department_id) VALUES (?, ?, ?)",
                "CS-B", "Beta", firstDepartmentId
        );

        jdbcTemplate.update(
                "INSERT INTO programs (code, name, department_id) VALUES (?, ?, ?)",
                "BUS-G", "Gamma", secondDepartmentId
        );

        adminToken = jwtService.generateToken(adminId);
    }

    @Test
    void listPrograms_returnsRequestedPage() throws Exception {
        mockMvc.perform(get("/api/v1/programs")
                        .header("Authorization", bearer(adminToken))
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Alpha"))
                .andExpect(jsonPath("$.content[1].name").value("Beta"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void listPrograms_filtersByDepartmentAndPagesResults() throws Exception {
        mockMvc.perform(get("/api/v1/programs")
                        .header("Authorization", bearer(adminToken))
                        .param("departmentId", firstDepartmentId.toString())
                        .param("page", "1")
                        .param("size", "1")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Beta"))
                .andExpect(jsonPath("$.content[0].departmentId")
                        .value(firstDepartmentId.toString()))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void listPrograms_returns404ForMissingDepartment() throws Exception {
        mockMvc.perform(get("/api/v1/programs")
                        .header("Authorization", bearer(adminToken))
                        .param("departmentId", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listPrograms_rejectsPageSizeOver100() throws Exception {
        mockMvc.perform(get("/api/v1/programs")
                        .header("Authorization", bearer(adminToken))
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listPrograms_rejectsUnsupportedSortField() throws Exception {
        mockMvc.perform(get("/api/v1/programs")
                        .header("Authorization", bearer(adminToken))
                        .param("sort", "unknown,asc"))
                .andExpect(status().isBadRequest());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
