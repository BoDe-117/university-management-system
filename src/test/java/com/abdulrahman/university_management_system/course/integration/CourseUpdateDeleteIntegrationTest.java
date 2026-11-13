package com.abdulrahman.university_management_system.course.integration;

import com.abdulrahman.university_management_system.course.dto.CourseResponse;
import com.abdulrahman.university_management_system.course.dto.CreateCourseRequest;
import com.abdulrahman.university_management_system.course.dto.UpdateCourseRequest;
import com.abdulrahman.university_management_system.course.exception.CourseCodeAlreadyExistsException;
import com.abdulrahman.university_management_system.course.exception.CourseNotFoundException;
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
class CourseUpdateDeleteIntegrationTest {

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
    void updateCourse_savesChangesAndMovesToAnotherProgram() {
        DepartmentResponse department = createDepartment();
        ProgramResponse first = createProgram("BSC-CS", department.id());
        ProgramResponse second = createProgram("BSC-SE", department.id());

        CourseResponse created = courseService.createCourse(
                new CreateCourseRequest("CS101", "Computing", 3, first.id())
        );

        courseService.updateCourse(
                created.id(),
                new UpdateCourseRequest(
                        "SE101",
                        "Software Engineering",
                        4,
                        second.id()
                )
        );

        CourseResponse retrieved = courseService.getCourseById(created.id());

        assertThat(retrieved.code()).isEqualTo("SE101");
        assertThat(retrieved.name()).isEqualTo("Software Engineering");
        assertThat(retrieved.credits()).isEqualTo(4);
        assertThat(retrieved.programId()).isEqualTo(second.id());
    }

    @Test
    void updateCourse_allowsKeepingSameCode() {
        DepartmentResponse department = createDepartment();
        ProgramResponse program = createProgram("BSC-CS", department.id());

        CourseResponse created = courseService.createCourse(
                new CreateCourseRequest("CS101", "Computing", 3, program.id())
        );

        courseService.updateCourse(
                created.id(),
                new UpdateCourseRequest("CS101", "Computing Basics", 4, program.id())
        );

        CourseResponse retrieved = courseService.getCourseById(created.id());

        assertThat(retrieved.code()).isEqualTo("CS101");
        assertThat(retrieved.name()).isEqualTo("Computing Basics");
        assertThat(retrieved.credits()).isEqualTo(4);
    }

    @Test
    void updateCourse_rejectsCodeUsedByAnotherCourse() {
        DepartmentResponse department = createDepartment();
        ProgramResponse program = createProgram("BSC-CS", department.id());

        courseService.createCourse(
                new CreateCourseRequest("CS101", "First Course", 3, program.id())
        );

        CourseResponse second = courseService.createCourse(
                new CreateCourseRequest("CS102", "Second Course", 4, program.id())
        );

        assertThatThrownBy(() -> courseService.updateCourse(
                second.id(),
                new UpdateCourseRequest("CS101", "Changed", 3, program.id())
        )).isInstanceOf(CourseCodeAlreadyExistsException.class);

        CourseResponse retrieved = courseService.getCourseById(second.id());
        assertThat(retrieved.code()).isEqualTo("CS102");
        assertThat(retrieved.name()).isEqualTo("Second Course");
    }

    @Test
    void deleteCourse_removesCourseAndAllowsCodeReuse() {
        DepartmentResponse department = createDepartment();
        ProgramResponse program = createProgram("BSC-CS", department.id());

        CourseResponse created = courseService.createCourse(
                new CreateCourseRequest("CS101", "Computing", 3, program.id())
        );

        courseService.deleteCourse(created.id());

        assertThatThrownBy(() -> courseService.getCourseById(created.id()))
                .isInstanceOf(CourseNotFoundException.class);

        CourseResponse replacement = courseService.createCourse(
                new CreateCourseRequest("CS101", "New Course", 3, program.id())
        );

        assertThat(replacement.id()).isNotEqualTo(created.id());
        assertThat(replacement.code()).isEqualTo("CS101");
    }

    @Test
    void deleteCourse_rejectsMissingCourse() {
        UUID missingId = UUID.randomUUID();

        assertThatThrownBy(() -> courseService.deleteCourse(missingId))
                .isInstanceOf(CourseNotFoundException.class);
    }

    private DepartmentResponse createDepartment() {
        return departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computer Science")
        );
    }

    private ProgramResponse createProgram(String code, UUID departmentId) {
        return programService.createProgram(
                new CreateProgramRequest(code, "Test Program", departmentId)
        );
    }
}
