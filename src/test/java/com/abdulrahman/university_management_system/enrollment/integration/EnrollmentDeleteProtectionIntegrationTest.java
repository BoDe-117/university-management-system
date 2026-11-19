package com.abdulrahman.university_management_system.enrollment.integration;

import com.abdulrahman.university_management_system.course.exception.CourseHasAssociatedEnrollmentsException;
import com.abdulrahman.university_management_system.course.service.CourseService;
import com.abdulrahman.university_management_system.enrollment.repository.EnrollmentRepository;
import com.abdulrahman.university_management_system.student.exception.StudentHasAssociatedEnrollmentsException;
import com.abdulrahman.university_management_system.student.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

@SpringBootTest
@Testcontainers
class EnrollmentDeleteProtectionIntegrationTest {

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
    private StudentService studentService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private EnrollmentRepository enrollmentRepository;

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

        jdbcTemplate.update(
                "INSERT INTO enrollments (student_id, course_id) VALUES (?, ?)",
                studentId, courseId
        );
    }

    @Test
    void deleteStudent_rejectsStudentWithEnrollment() {
        assertThatThrownBy(() -> studentService.deleteStudent(studentId))
                .isInstanceOf(StudentHasAssociatedEnrollmentsException.class);

        assertThat(studentExists()).isTrue();
    }

    @Test
    void deleteCourse_rejectsCourseWithEnrollment() {
        assertThatThrownBy(() -> courseService.deleteCourse(courseId))
                .isInstanceOf(CourseHasAssociatedEnrollmentsException.class);

        assertThat(courseExists()).isTrue();
    }

    @Test
    void deleteStudent_translatesDatabaseForeignKeyViolation() {
        doReturn(false).when(enrollmentRepository).existsByStudentId(studentId);

        assertThatThrownBy(() -> studentService.deleteStudent(studentId))
                .isInstanceOf(StudentHasAssociatedEnrollmentsException.class);

        assertThat(studentExists()).isTrue();
    }

    @Test
    void deleteCourse_translatesDatabaseForeignKeyViolation() {
        doReturn(false).when(enrollmentRepository).existsByCourseId(courseId);

        assertThatThrownBy(() -> courseService.deleteCourse(courseId))
                .isInstanceOf(CourseHasAssociatedEnrollmentsException.class);

        assertThat(courseExists()).isTrue();
    }

    @Test
    void studentAndCourse_canBeDeletedWhenNoEnrollmentExists() {
        jdbcTemplate.update("DELETE FROM enrollments");

        studentService.deleteStudent(studentId);
        courseService.deleteCourse(courseId);

        assertThat(studentExists()).isFalse();
        assertThat(courseExists()).isFalse();
    }

    private boolean studentExists() {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM students WHERE id = ?)",
                Boolean.class,
                studentId
        );

        return Boolean.TRUE.equals(exists);
    }

    private boolean courseExists() {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM courses WHERE id = ?)",
                Boolean.class,
                courseId
        );

        return Boolean.TRUE.equals(exists);
    }
}
