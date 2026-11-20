package com.abdulrahman.university_management_system.security.integration;

import com.abdulrahman.university_management_system.security.jwt.JwtService;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RbacOwnershipIntegrationTest {

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
    private UUID firstEnrollmentId;
    private UUID secondEnrollmentId;
    private UUID newCourseId;

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
        UUID courseId = UUID.randomUUID();
        newCourseId = UUID.randomUUID();
        firstStudentId = UUID.randomUUID();
        secondStudentId = UUID.randomUUID();
        firstEnrollmentId = UUID.randomUUID();
        secondEnrollmentId = UUID.randomUUID();

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
                "INSERT INTO courses (id, code, name, credits, program_id) VALUES (?, ?, ?, ?, ?)",
                courseId, "CS101", "Programming", 3, programId
        );

        jdbcTemplate.update(
                "INSERT INTO courses (id, code, name, credits, program_id) VALUES (?, ?, ?, ?, ?)",
                newCourseId, "CS102", "Databases", 3, programId
        );

        jdbcTemplate.update(
                "INSERT INTO students (id, student_id, first_name, last_name, email, program_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                firstStudentId, "STU-001", "Sara", "Ahmed", "sara@example.com", programId
        );

        jdbcTemplate.update(
                "INSERT INTO students (id, student_id, first_name, last_name, email, program_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                secondStudentId, "STU-002", "Omar", "Ali", "omar@example.com", programId
        );

        jdbcTemplate.update(
                "INSERT INTO enrollments (id, student_id, course_id) VALUES (?, ?, ?)",
                firstEnrollmentId, firstStudentId, courseId
        );

        jdbcTemplate.update(
                "INSERT INTO enrollments (id, student_id, course_id) VALUES (?, ?, ?)",
                secondEnrollmentId, secondStudentId, courseId
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
                studentUserId, "sara", "test-hash", "STUDENT", firstStudentId
        );

        adminToken = jwtService.generateToken(adminId);
        staffToken = jwtService.generateToken(staffId);
        studentToken = jwtService.generateToken(studentUserId);
    }

    @Test
    void requestWithoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/students/{id}", firstStudentId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void student_canBrowseCourses() throws Exception {
        mockMvc.perform(get("/api/v1/courses")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk());
    }

    @Test
    void student_canReadOwnRecord() throws Exception {
        mockMvc.perform(get("/api/v1/students/{id}", firstStudentId)
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstStudentId.toString()));
    }

    @Test
    void student_cannotReadAnotherStudentsRecord() throws Exception {
        mockMvc.perform(get("/api/v1/students/{id}", secondStudentId)
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void student_cannotListAllStudents() throws Exception {
        mockMvc.perform(get("/api/v1/students")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void staff_canListStudents() throws Exception {
        mockMvc.perform(get("/api/v1/students")
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk());
    }

    @Test
    void staff_cannotDeleteStudent() throws Exception {
        mockMvc.perform(delete("/api/v1/students/{id}", secondStudentId)
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_canReachDeleteEndpoint() throws Exception {
        UUID missingStudentId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/students/{id}", missingStudentId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void student_canReadOwnEnrollment() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments/{id}", firstEnrollmentId)
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstEnrollmentId.toString()));
    }

    @Test
    void student_cannotReadAnotherStudentsEnrollment() throws Exception {
        mockMvc.perform(get("/api/v1/enrollments/{id}", secondEnrollmentId)
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void student_canEnrollThemselves() throws Exception {
        mockMvc.perform(post("/api/v1/enrollments")
                        .header("Authorization", bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(enrollmentRequest(firstStudentId, newCourseId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value(firstStudentId.toString()));
    }

    @Test
    void student_cannotEnrollAnotherStudent() throws Exception {
        mockMvc.perform(post("/api/v1/enrollments")
                        .header("Authorization", bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(enrollmentRequest(secondStudentId, newCourseId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void student_canDropOwnEnrollment() throws Exception {
        mockMvc.perform(post("/api/v1/enrollments/{id}/drop", firstEnrollmentId)
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DROPPED"));
    }

    @Test
    void student_cannotDropAnotherStudentsEnrollment() throws Exception {
        mockMvc.perform(post("/api/v1/enrollments/{id}/drop", secondEnrollmentId)
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void student_cannotCompleteEnrollment() throws Exception {
        mockMvc.perform(post("/api/v1/enrollments/{id}/complete", firstEnrollmentId)
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void staff_canCompleteEnrollment() throws Exception {
        mockMvc.perform(post("/api/v1/enrollments/{id}/complete", firstEnrollmentId)
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String enrollmentRequest(UUID studentId, UUID courseId) {
        return """
                {
                    "studentId": "%s",
                    "courseId": "%s"
                }
                """.formatted(studentId, courseId);
    }
}
