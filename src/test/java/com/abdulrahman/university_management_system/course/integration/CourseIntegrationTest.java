package com.abdulrahman.university_management_system.course.integration;

import com.abdulrahman.university_management_system.course.dto.CourseResponse;
import com.abdulrahman.university_management_system.course.dto.CreateCourseRequest;
import com.abdulrahman.university_management_system.course.exception.CourseCodeAlreadyExistsException;
import com.abdulrahman.university_management_system.course.service.CourseService;
import com.abdulrahman.university_management_system.department.dto.CreateDepartmentRequest;
import com.abdulrahman.university_management_system.department.dto.DepartmentResponse;
import com.abdulrahman.university_management_system.department.service.DepartmentService;
import com.abdulrahman.university_management_system.program.dto.CreateProgramRequest;
import com.abdulrahman.university_management_system.program.dto.ProgramResponse;
import com.abdulrahman.university_management_system.program.service.ProgramService;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class CourseIntegrationTest {

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
    private CourseService courseService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM courses");
        jdbcTemplate.update("DELETE FROM programs");
        jdbcTemplate.update("DELETE FROM departments");
    }

    @Test
    void createAndRetrieveCourse_withItsProgram() {
        ProgramResponse program = createProgram("BSC-CS", "Computer Science");

        CourseResponse created = courseService.createCourse(
                new CreateCourseRequest(
                        "CS101",
                        "Introduction to Computing",
                        3,
                        program.id()
                )
        );

        CourseResponse retrieved = courseService.getCourseById(created.id());

        assertThat(created.id()).isNotNull();
        assertThat(retrieved.code()).isEqualTo("CS101");
        assertThat(retrieved.name()).isEqualTo("Introduction to Computing");
        assertThat(retrieved.credits()).isEqualTo(3);
        assertThat(retrieved.programId()).isEqualTo(program.id());
        assertThat(retrieved.programCode()).isEqualTo("BSC-CS");
        assertThat(retrieved.createdAt()).isNotNull();
        assertThat(retrieved.updatedAt()).isNotNull();
    }

    @Test
    void createCourse_rejectsDuplicateCode() {
        ProgramResponse program = createProgram("BSC-CS", "Computer Science");

        courseService.createCourse(
                new CreateCourseRequest("CS101", "Computing", 3, program.id())
        );

        assertThatThrownBy(() -> courseService.createCourse(
                new CreateCourseRequest("CS101", "Another Course", 4, program.id())
        )).isInstanceOf(CourseCodeAlreadyExistsException.class);
    }

    @Test
    void getCoursesByProgram_returnsOnlyMatchingCourses() {
        DepartmentResponse department = departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computer Science")
        );

        ProgramResponse first = programService.createProgram(
                new CreateProgramRequest("BSC-CS", "Computer Science", department.id())
        );

        ProgramResponse second = programService.createProgram(
                new CreateProgramRequest("BSC-SE", "Software Engineering", department.id())
        );

        courseService.createCourse(
                new CreateCourseRequest("CS101", "Computing", 3, first.id())
        );

        courseService.createCourse(
                new CreateCourseRequest("SE101", "Software Engineering", 4, second.id())
        );

        List<CourseResponse> courses =
                courseService.getCoursesByProgramId(first.id());

        assertThat(courses).hasSize(1);
        assertThat(courses.getFirst().code()).isEqualTo("CS101");
        assertThat(courses.getFirst().programId()).isEqualTo(first.id());
    }

    @Test
    void databaseRejectsCreditsOutsideAllowedRange() {
        ProgramResponse program = createProgram("BSC-CS", "Computer Science");

        assertThatThrownBy(() -> insertCourse("LOW", 0, program.id()))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> insertCourse("HIGH", 13, program.id()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseAcceptsCreditsAtBothBoundaries() {
        ProgramResponse program = createProgram("BSC-CS", "Computer Science");

        insertCourse("MIN", 1, program.id());
        insertCourse("MAX", 12, program.id());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM courses WHERE program_id = ?",
                Integer.class,
                program.id()
        );

        assertThat(count).isEqualTo(2);
    }

    private ProgramResponse createProgram(String code, String name) {
        DepartmentResponse department = departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computer Science")
        );

        return programService.createProgram(
                new CreateProgramRequest(code, name, department.id())
        );
    }

    private void insertCourse(String code, int credits, UUID programId) {
        jdbcTemplate.update(
                """
                INSERT INTO courses (id, code, name, credits, program_id)
                VALUES (?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                code,
                "Test Course",
                credits,
                programId
        );
    }
}
