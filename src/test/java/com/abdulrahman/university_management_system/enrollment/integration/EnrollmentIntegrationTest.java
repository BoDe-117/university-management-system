package com.abdulrahman.university_management_system.enrollment.integration;

import com.abdulrahman.university_management_system.enrollment.dto.CreateEnrollmentRequest;
import com.abdulrahman.university_management_system.enrollment.dto.EnrollmentResponse;
import com.abdulrahman.university_management_system.enrollment.entity.EnrollmentStatus;
import com.abdulrahman.university_management_system.enrollment.exception.EnrollmentAlreadyExistsException;
import com.abdulrahman.university_management_system.enrollment.service.EnrollmentService;
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
class EnrollmentIntegrationTest {

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
    private EnrollmentService enrollmentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID studentId;
    private UUID courseId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM enrollments");
        jdbcTemplate.update("DELETE FROM students");
        jdbcTemplate.update("DELETE FROM courses");
        jdbcTemplate.update("DELETE FROM programs");
        jdbcTemplate.update("DELETE FROM departments");

        UUID departmentId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        courseId = UUID.randomUUID();

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

        jdbcTemplate.update(
                "INSERT INTO courses (id, code, name, credits, program_id) VALUES (?, ?, ?, ?, ?)",
                courseId, "CS101", "Introduction to Programming", 3, programId
        );
    }

    @Test
    void createAndRetrieveEnrollment() {
        EnrollmentResponse created = enrollmentService.createEnrollment(
                new CreateEnrollmentRequest(studentId, courseId)
        );

        EnrollmentResponse retrieved =
                enrollmentService.getEnrollmentById(created.id());

        assertThat(retrieved.id()).isEqualTo(created.id());
        assertThat(retrieved.studentId()).isEqualTo(studentId);
        assertThat(retrieved.courseId()).isEqualTo(courseId);
        assertThat(retrieved.status()).isEqualTo(EnrollmentStatus.ENROLLED);
        assertThat(retrieved.enrolledAt()).isNotNull();
        assertThat(retrieved.createdAt()).isNotNull();
        assertThat(retrieved.updatedAt()).isNotNull();
    }

    @Test
    void createEnrollment_rejectsDuplicate() {
        CreateEnrollmentRequest request =
                new CreateEnrollmentRequest(studentId, courseId);

        enrollmentService.createEnrollment(request);

        assertThatThrownBy(() -> enrollmentService.createEnrollment(request))
                .isInstanceOf(EnrollmentAlreadyExistsException.class);
    }

    @Test
    void database_rejectsDuplicateStudentAndCourse() {
        jdbcTemplate.update(
                "INSERT INTO enrollments (student_id, course_id) VALUES (?, ?)",
                studentId, courseId
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO enrollments (student_id, course_id) VALUES (?, ?)",
                studentId, courseId
        )).isInstanceOf(DataIntegrityViolationException.class);
    }
}
