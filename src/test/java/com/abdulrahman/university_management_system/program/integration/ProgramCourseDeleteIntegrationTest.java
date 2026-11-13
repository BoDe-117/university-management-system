package com.abdulrahman.university_management_system.program.integration;

import com.abdulrahman.university_management_system.course.repository.CourseRepository;
import com.abdulrahman.university_management_system.department.dto.CreateDepartmentRequest;
import com.abdulrahman.university_management_system.department.dto.DepartmentResponse;
import com.abdulrahman.university_management_system.program.dto.CreateProgramRequest;
import com.abdulrahman.university_management_system.program.dto.ProgramResponse;
import com.abdulrahman.university_management_system.program.exception.ProgramHasAssociatedCoursesException;
import com.abdulrahman.university_management_system.department.service.DepartmentService;
import com.abdulrahman.university_management_system.program.service.ProgramService;
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
class ProgramCourseDeleteIntegrationTest {

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
    private DepartmentService departmentService;

    @Autowired
    private ProgramService programService;

    @MockitoSpyBean
    private CourseRepository courseRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM courses");
        jdbcTemplate.update("DELETE FROM programs");
        jdbcTemplate.update("DELETE FROM departments");
    }

    @Test
    void deleteProgram_rejectsProgramWithCourses() {
        ProgramResponse program = createProgram();
        insertCourse(program.id());

        assertThatThrownBy(() -> programService.deleteProgram(program.id()))
                .isInstanceOf(ProgramHasAssociatedCoursesException.class)
                .hasMessageContaining("BSC-CS");

        assertThat(programService.getProgramById(program.id()).code())
                .isEqualTo("BSC-CS");

        assertThat(courseRepository.existsByProgramId(program.id())).isTrue();
    }

    @Test
    void deleteProgram_translatesForeignKeyViolation() {
        ProgramResponse program = createProgram();
        insertCourse(program.id());

        // Simulate a course appearing after the initial existence check.
        doReturn(false).when(courseRepository)
                .existsByProgramId(program.id());

        assertThatThrownBy(() -> programService.deleteProgram(program.id()))
                .isInstanceOf(ProgramHasAssociatedCoursesException.class);

        assertThat(programService.getProgramById(program.id()).code())
                .isEqualTo("BSC-CS");

        Integer courseCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM courses WHERE program_id = ?",
                Integer.class,
                program.id()
        );

        assertThat(courseCount).isEqualTo(1);
    }

    private ProgramResponse createProgram() {
        DepartmentResponse department = departmentService.createDepartment(
                new CreateDepartmentRequest("CS", "Computer Science")
        );

        return programService.createProgram(
                new CreateProgramRequest(
                        "BSC-CS",
                        "Computer Science",
                        department.id()
                )
        );
    }

    private void insertCourse(UUID programId) {
        jdbcTemplate.update(
                """
                INSERT INTO courses (id, code, name, credits, program_id)
                VALUES (?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                "CS101",
                "Introduction to Computing",
                3,
                programId
        );
    }
}
