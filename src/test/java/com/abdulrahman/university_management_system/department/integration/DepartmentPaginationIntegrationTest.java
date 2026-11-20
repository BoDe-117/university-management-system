package com.abdulrahman.university_management_system.department.integration;

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
class DepartmentPaginationIntegrationTest {

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

    private String adminToken;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("DELETE FROM enrollments");
        jdbcTemplate.update("DELETE FROM students");
        jdbcTemplate.update("DELETE FROM courses");
        jdbcTemplate.update("DELETE FROM programs");
        jdbcTemplate.update("DELETE FROM departments");

        UUID adminId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO users (id, username, password_hash, role) VALUES (?, ?, ?, ?)",
                adminId, "admin", "test-hash", "ADMIN"
        );

        jdbcTemplate.update(
                "INSERT INTO departments (code, name) VALUES (?, ?)",
                "CS", "Computer Science"
        );

        jdbcTemplate.update(
                "INSERT INTO departments (code, name) VALUES (?, ?)",
                "SE", "Software Engineering"
        );

        jdbcTemplate.update(
                "INSERT INTO departments (code, name) VALUES (?, ?)",
                "IT", "Information Technology"
        );

        adminToken = jwtService.generateToken(adminId);
    }

    @Test
    void listDepartments_returnsRequestedPage() throws Exception {
        mockMvc.perform(get("/api/v1/departments")
                        .header("Authorization", bearer(adminToken))
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Computer Science"))
                .andExpect(jsonPath("$.content[1].name").value("Information Technology"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void listDepartments_returnsSecondPage() throws Exception {
        mockMvc.perform(get("/api/v1/departments")
                        .header("Authorization", bearer(adminToken))
                        .param("page", "1")
                        .param("size", "2")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Software Engineering"))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void listDepartments_rejectsPageSizeOver100() throws Exception {
        mockMvc.perform(get("/api/v1/departments")
                        .header("Authorization", bearer(adminToken))
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void listDepartments_rejectsUnsupportedSortField() throws Exception {
        mockMvc.perform(get("/api/v1/departments")
                        .header("Authorization", bearer(adminToken))
                        .param("sort", "unknown,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
