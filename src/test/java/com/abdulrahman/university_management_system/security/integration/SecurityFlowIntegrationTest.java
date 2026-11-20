package com.abdulrahman.university_management_system.security.integration;

import com.abdulrahman.university_management_system.user.dto.CreateUserRequest;
import com.abdulrahman.university_management_system.user.entity.UserRole;
import com.abdulrahman.university_management_system.user.service.UserService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SecurityFlowIntegrationTest {

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
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM users");

        userService.createUser(
                new CreateUserRequest(
                        "test-admin",
                        "ExamplePassword123!",
                        UserRole.ADMIN,
                        null
                )
        );
    }

    @Test
    void login_isAvailableWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_rejectsWrongPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "username": "test-admin",
                                    "password": "WrongPassword123!"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_rejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/departments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_rejectsInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/departments")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validAdminToken_canAccessDepartments() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLogin()))
                .andExpect(status().isOk())
                .andReturn();

        String token = JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.accessToken"
        );

        mockMvc.perform(get("/api/v1/departments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String validLogin() {
        return """
                {
                    "username": "test-admin",
                    "password": "ExamplePassword123!"
                }
                """;
    }
}
