package com.abdulrahman.university_management_system.security.integration;

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
class EnrollmentListingSecurityIntegrationTest {

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

    private UUID firstStudentId;
    private UUID secondStudentId;
    private UUID firstCourseId;
    private UUID secondCourseId;

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
        firstStudentId = UUID.randomUUID();
        secondStudentId = UUID.randomUUID();
        firstCourseId = UUID.randomUUID();
        secondCourseId = UUID.randomUUID();

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
                firstStudentId, "STU-001", "Sara", "Ahmed",
                "sara@example.com", programId
        );

        jdbcTemplate.update(
                "INSERT INTO students (id, student_id, first_name, last_name, email, program_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                secondStudentId, "STU-002", "Omar", "Ali",
                "omar@example.com", programId
        );

        jdbcTemplate.update(
                "INSERT INTO courses (id, code, name, credits, program_id) VALUES (?, ?, ?, ?, ?)",
                firstCourseId, "CS101", "Programming", 3, programId
        );

        jdbcTemplate.update(
                "INSERT INTO courses (id, code, name, credits, program_id) VALUES (?, ?, ?, ?, ?)",
                secondCourseId, "CS102", "Databases", 3, programId
        );

        jdbcTemplate.update(
                "INSERT INTO enrollments (student_id, course_id) VALUES (?, ?)",
                firstStudentId, firstCourseId
        );

        jdbcTemplate.update(
                "INSERT INTO enrollments (student_id, course_id) VALUES (?, ?)",
                firstStudentId, secondCourseId
        );

        jdbcTemplate.update(
                "INSERT INTO enrollments (student_id, course_id) VALUES (?, ?)",
                secondStudentId, firstCourseId
        );

        jdbcTemplate.update(
                "INSERT INTO users (id, username, password_hash, role) VALUES (?, ?, ?, ?)",
                staffId, "staff", "test-hash", "STAFF"
        );

        jdbcTemplate.update(
                "INSERT INTO users (id, username, password_hash, role, student_id) VALUES (?, ?, ?, ?, ?)",
                studentUserId, "sara", "test-hash", "STUDENT", firstStudentId
        );

        staffToken = jwtService.generateToken(staffId);
        studentToken = jwtService.generateToken(studentUserId);
    }

    @Test
    void withoutToken_cannotListEnrollments() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staff_canListAllEnrollments() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments")
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content.length()").value(3));
    }

    @Test
    void staff_canFilterByStudentAndCourse() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments")
                        .header("Authorization", bearer(staffToken))
                        .param("studentId", secondStudentId.toString())
                        .param("courseId", firstCourseId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].studentId")
                        .value(secondStudentId.toString()));
    }

    @Test
    void staff_canRequestPages() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments")
                        .header("Authorization", bearer(staffToken))
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void student_withoutFilter_seesOnlyOwnEnrollments() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].studentId")
                        .value(firstStudentId.toString()))
                .andExpect(jsonPath("$.content[1].studentId")
                        .value(firstStudentId.toString()));
    }

    @Test
    void student_canFilterOwnEnrollmentsByCourse() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments")
                        .header("Authorization", bearer(studentToken))
                        .param("courseId", secondCourseId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].studentId")
                        .value(firstStudentId.toString()))
                .andExpect(jsonPath("$.content[0].courseId")
                        .value(secondCourseId.toString()));
    }

    @Test
    void student_cannotRequestAnotherStudentsEnrollments() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments")
                        .header("Authorization", bearer(studentToken))
                        .param("studentId", secondStudentId.toString()))
                .andExpect(status().isForbidden());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
