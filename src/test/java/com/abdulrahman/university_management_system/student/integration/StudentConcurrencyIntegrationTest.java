package com.abdulrahman.university_management_system.student.integration;

import com.abdulrahman.university_management_system.department.dto.CreateDepartmentRequest;
import com.abdulrahman.university_management_system.department.dto.DepartmentResponse;
import com.abdulrahman.university_management_system.department.service.DepartmentService;
import com.abdulrahman.university_management_system.program.dto.CreateProgramRequest;
import com.abdulrahman.university_management_system.program.dto.ProgramResponse;
import com.abdulrahman.university_management_system.program.service.ProgramService;
import com.abdulrahman.university_management_system.student.dto.CreateStudentRequest;
import com.abdulrahman.university_management_system.student.dto.StudentResponse;
import com.abdulrahman.university_management_system.student.exception.StudentEmailAlreadyExistsException;
import com.abdulrahman.university_management_system.student.exception.StudentIdAlreadyExistsException;
import com.abdulrahman.university_management_system.student.repository.StudentRepository;
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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@Testcontainers
class StudentConcurrencyIntegrationTest {

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

    @Autowired
    private StudentService studentService;

    @MockitoSpyBean
    private StudentRepository studentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM students");
        jdbcTemplate.update("DELETE FROM courses");
        jdbcTemplate.update("DELETE FROM programs");
        jdbcTemplate.update("DELETE FROM departments");
    }

    @Test
    void concurrentCreatesWithSameStudentIdProduceOneConflict()
            throws Exception {
        ProgramResponse program = createProgram();
        CyclicBarrier barrier = new CyclicBarrier(2);

        doAnswer(invocation -> {
            Boolean exists = jdbcTemplate.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM students WHERE student_id = ?)",
                    Boolean.class,
                    "STU-RACE"
            );

            assertThat(exists).isFalse();
            barrier.await(10, TimeUnit.SECONDS);
            return exists;
        }).when(studentRepository).existsByStudentId("STU-RACE");

        CreateStudentRequest first = request(
                "STU-RACE", "first@example.com", program
        );

        CreateStudentRequest second = request(
                "STU-RACE", "second@example.com", program
        );

        runConcurrentCreates(first, second, StudentIdAlreadyExistsException.class);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM students WHERE student_id = ?",
                Integer.class,
                "STU-RACE"
        );

        assertThat(count).isEqualTo(1);
    }

    @Test
    void concurrentCreatesWithSameEmailProduceOneConflict()
            throws Exception {
        ProgramResponse program = createProgram();
        CyclicBarrier barrier = new CyclicBarrier(2);

        doAnswer(invocation -> {
            Boolean exists = jdbcTemplate.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM students WHERE email = ?)",
                    Boolean.class,
                    "race@example.com"
            );

            assertThat(exists).isFalse();
            barrier.await(10, TimeUnit.SECONDS);
            return exists;
        }).when(studentRepository).existsByEmail("race@example.com");

        CreateStudentRequest first = request(
                "STU-001", "race@example.com", program
        );

        CreateStudentRequest second = request(
                "STU-002", "race@example.com", program
        );

        runConcurrentCreates(first, second, StudentEmailAlreadyExistsException.class);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM students WHERE email = ?",
                Integer.class,
                "race@example.com"
        );

        assertThat(count).isEqualTo(1);
    }

    private void runConcurrentCreates(
            CreateStudentRequest first,
            CreateStudentRequest second,
            Class<? extends RuntimeException> expectedException
    ) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch finished = new CountDownLatch(2);

        List<StudentResponse> successes = new CopyOnWriteArrayList<>();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        try {
            for (CreateStudentRequest request : List.of(first, second)) {
                executor.submit(() -> {
                    try {
                        successes.add(studentService.createStudent(request));
                    } catch (Throwable failure) {
                        failures.add(failure);
                    } finally {
                        finished.countDown();
                    }
                });
            }

            assertThat(finished.await(30, TimeUnit.SECONDS))
                    .as("Both student creation requests should finish")
                    .isTrue();

            assertThat(successes).hasSize(1);
            assertThat(failures).hasSize(1);
            assertThat(failures.getFirst()).isInstanceOf(expectedException);
        } finally {
            executor.shutdownNow();

            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS))
                    .as("Worker threads should terminate")
                    .isTrue();
        }
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

    private CreateStudentRequest request(
            String studentId,
            String email,
            ProgramResponse program
    ) {
        return new CreateStudentRequest(
                studentId,
                "Sara",
                "Ahmed",
                email,
                program.id()
        );
    }
}
