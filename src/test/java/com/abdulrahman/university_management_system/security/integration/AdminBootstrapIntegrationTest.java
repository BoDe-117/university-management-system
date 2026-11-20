package com.abdulrahman.university_management_system.security.integration;

import com.abdulrahman.university_management_system.security.bootstrap.AdminBootstrapService;
import com.abdulrahman.university_management_system.user.entity.UserRole;
import com.abdulrahman.university_management_system.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class AdminBootstrapIntegrationTest {

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
    private AdminBootstrapService bootstrapService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void createsFirstAdminWithEncodedPassword() {
        String password = "ExamplePassword123!";

        boolean created = bootstrapService.createFirstAdmin(
                "first-admin",
                password
        );

        String storedHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE username = ?",
                String.class,
                "first-admin"
        );

        assertThat(created).isTrue();
        assertThat(userRepository.existsByRole(UserRole.ADMIN)).isTrue();
        assertThat(storedHash).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, storedHash)).isTrue();
    }

    @Test
    void doesNotCreateAnotherAdminWhenOneAlreadyExists() {
        bootstrapService.createFirstAdmin("first-admin", "ExamplePassword123!");

        boolean created = bootstrapService.createFirstAdmin(
                "second-admin",
                "AnotherPassword123!"
        );

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'",
                Integer.class
        );

        assertThat(created).isFalse();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void rejectsShortPassword() {
        assertThatThrownBy(() ->
                bootstrapService.createFirstAdmin("first-admin", "short")
        ).isInstanceOf(IllegalArgumentException.class);

        assertThat(userRepository.existsByRole(UserRole.ADMIN)).isFalse();
    }

    @Test
    void concurrentBootstrapAttempts_createOnlyOneAdmin() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(2);

        List<Boolean> results = new CopyOnWriteArrayList<>();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        try {
            for (int i = 0; i < 2; i++) {
                int number = i;

                executor.submit(() -> {
                    try {
                        ready.countDown();

                        if (!start.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Start signal timed out");
                        }

                        results.add(bootstrapService.createFirstAdmin(
                                "admin-" + number,
                                "ExamplePassword123!"
                        ));
                    } catch (Throwable failure) {
                        failures.add(failure);
                    } finally {
                        finished.countDown();
                    }
                });
            }

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(finished.await(30, TimeUnit.SECONDS)).isTrue();
            assertThat(failures).isEmpty();
            assertThat(results).containsExactlyInAnyOrder(true, false);

            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'",
                    Integer.class
            );

            assertThat(count).isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }
}
