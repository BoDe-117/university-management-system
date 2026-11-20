package com.abdulrahman.university_management_system.security.integration;

import com.abdulrahman.university_management_system.security.dto.LoginRequest;
import com.abdulrahman.university_management_system.security.dto.TokenResponse;
import com.abdulrahman.university_management_system.security.jwt.JwtService;
import com.abdulrahman.university_management_system.security.service.AuthService;
import com.abdulrahman.university_management_system.user.entity.User;
import com.abdulrahman.university_management_system.user.entity.UserRole;
import com.abdulrahman.university_management_system.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("bootstrap-admin")
@Testcontainers
class AdminBootstrapStartupIntegrationTest {

    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Test-only values: never use these for a real deployment.
        registry.add(
                "JWT_SECRET",
                () -> "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
        );
        registry.add("BOOTSTRAP_ADMIN_USERNAME", () -> "test-admin");
        registry.add("BOOTSTRAP_ADMIN_PASSWORD", () -> "ExamplePassword123!");
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Test
    void bootstrapProfile_createsAdminWhoCanLogIn() {
        User admin = userRepository.findByUsername("test-admin")
                .orElseThrow();

        assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(admin.getPasswordHash()).isNotEqualTo("ExamplePassword123!");
        assertThat(passwordEncoder.matches(
                "ExamplePassword123!",
                admin.getPasswordHash()
        )).isTrue();

        TokenResponse response = authService.login(
                new LoginRequest("test-admin", "ExamplePassword123!")
        );

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(jwtService.extractUserId(response.accessToken()))
                .isEqualTo(admin.getId());
    }
}
