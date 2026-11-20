package com.abdulrahman.university_management_system.user.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class UserIntegrationTest {

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
    private JdbcTemplate jdbcTemplate;

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
    void studentAccount_canBeLinkedToStudent() {
        jdbcTemplate.update(
                "INSERT INTO users (username, password_hash, role, student_id) VALUES (?, ?, ?, ?)",
                "sara", "test-hash", "STUDENT", studentId
        );

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE student_id = ?",
                Integer.class,
                studentId
        );

        assertThat(count).isEqualTo(1);
    }

    @Test
    void studentAccount_requiresStudent() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
                "sara", "test-hash", "STUDENT"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void student_cannotHaveTwoAccounts() {
        jdbcTemplate.update(
                "INSERT INTO users (username, password_hash, role, student_id) VALUES (?, ?, ?, ?)",
                "sara", "test-hash", "STUDENT", studentId
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO users (username, password_hash, role, student_id) VALUES (?, ?, ?, ?)",
                "second-account", "test-hash", "STUDENT", studentId
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void adminAccount_cannotBeLinkedToStudent() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO users (username, password_hash, role, student_id) VALUES (?, ?, ?, ?)",
                "admin", "test-hash", "ADMIN", studentId
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void usernames_mustBeUnique() {
        jdbcTemplate.update(
                "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
                "admin", "test-hash", "ADMIN"
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
                "admin", "test-hash", "STAFF"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }
}
