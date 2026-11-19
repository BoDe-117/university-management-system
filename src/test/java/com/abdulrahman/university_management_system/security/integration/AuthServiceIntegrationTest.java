package com.abdulrahman.university_management_system.security.integration;

import com.abdulrahman.university_management_system.security.dto.LoginRequest;
import com.abdulrahman.university_management_system.security.dto.TokenResponse;
import com.abdulrahman.university_management_system.security.jwt.JwtService;
import com.abdulrahman.university_management_system.security.service.AuthService;
import com.abdulrahman.university_management_system.user.dto.CreateUserRequest;
import com.abdulrahman.university_management_system.user.dto.UserResponse;
import com.abdulrahman.university_management_system.user.entity.UserRole;
import com.abdulrahman.university_management_system.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class AuthServiceIntegrationTest {

    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Test-only key: do not use this value for a real deployment.
        registry.add(
                "JWT_SECRET",
                () -> "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
        );
    }

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UserResponse account;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM users");

        account = userService.createUser(
                new CreateUserRequest(
                        "test-admin",
                        "ExamplePassword123!",
                        UserRole.ADMIN,
                        null
                )
        );
    }

    @Test
    void login_withCorrectPasswordReturnsTokenForAccount() {
        TokenResponse response = authService.login(
                new LoginRequest("test-admin", "ExamplePassword123!")
        );

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(jwtService.extractUserId(response.accessToken()))
                .isEqualTo(account.id());
    }

    @Test
    void login_withWrongPasswordIsRejected() {
        assertThatThrownBy(() -> authService.login(
                new LoginRequest("test-admin", "WrongPassword123!")
        )).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_withUnknownUsernameIsRejected() {
        assertThatThrownBy(() -> authService.login(
                new LoginRequest("unknown", "ExamplePassword123!")
        )).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_withPasswordOver72BytesIsRejected() {
        assertThatThrownBy(() -> authService.login(
                new LoginRequest("test-admin", "a".repeat(73))
        )).isInstanceOf(BadCredentialsException.class);
    }
}
