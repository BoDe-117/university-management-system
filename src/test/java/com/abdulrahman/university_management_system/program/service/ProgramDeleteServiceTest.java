package com.abdulrahman.university_management_system.program.service;

import com.abdulrahman.university_management_system.course.repository.CourseRepository;
import com.abdulrahman.university_management_system.department.entity.Department;
import com.abdulrahman.university_management_system.department.repository.DepartmentRepository;
import com.abdulrahman.university_management_system.program.entity.Program;
import com.abdulrahman.university_management_system.program.exception.ProgramHasAssociatedCoursesException;
import com.abdulrahman.university_management_system.program.exception.ProgramHasAssociatedStudentsException;
import com.abdulrahman.university_management_system.program.exception.ProgramNotFoundException;
import com.abdulrahman.university_management_system.program.repository.ProgramRepository;
import com.abdulrahman.university_management_system.student.repository.StudentRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgramDeleteServiceTest {

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private ProgramService programService;

    @Test
    void deleteProgram_deletesProgram_whenNoCoursesOrStudentsExist() {
        UUID programId = UUID.randomUUID();
        Program program = createProgram();

        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));
        when(courseRepository.existsByProgramId(programId))
                .thenReturn(false);
        when(studentRepository.existsByProgramId(programId))
                .thenReturn(false);

        programService.deleteProgram(programId);

        verify(programRepository).delete(program);
        verify(programRepository).flush();
    }

    @Test
    void deleteProgram_throwsNotFound_whenProgramDoesNotExist() {
        UUID programId = UUID.randomUUID();

        when(programRepository.findById(programId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> programService.deleteProgram(programId))
                .isInstanceOf(ProgramNotFoundException.class);

        verify(courseRepository, never()).existsByProgramId(programId);
        verify(studentRepository, never()).existsByProgramId(programId);
        verify(programRepository, never()).delete(any(Program.class));
    }

    @Test
    void deleteProgram_throwsConflict_whenCoursesExist() {
        UUID programId = UUID.randomUUID();
        Program program = createProgram();

        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));
        when(courseRepository.existsByProgramId(programId))
                .thenReturn(true);

        assertThatThrownBy(() -> programService.deleteProgram(programId))
                .isInstanceOf(ProgramHasAssociatedCoursesException.class)
                .hasMessageContaining("BSC-CS");

        verify(programRepository, never()).delete(program);
        verify(programRepository, never()).flush();
    }

    @Test
    void deleteProgram_throwsConflict_whenStudentsExist() {
        UUID programId = UUID.randomUUID();
        Program program = createProgram();

        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));
        when(courseRepository.existsByProgramId(programId))
                .thenReturn(false);
        when(studentRepository.existsByProgramId(programId))
                .thenReturn(true);

        assertThatThrownBy(() -> programService.deleteProgram(programId))
                .isInstanceOf(ProgramHasAssociatedStudentsException.class)
                .hasMessageContaining("BSC-CS");

        verify(programRepository, never()).delete(program);
        verify(programRepository, never()).flush();
    }

    @Test
    void deleteProgram_throwsConflict_whenCourseConstraintRejectsDelete() {
        UUID programId = UUID.randomUUID();
        Program program = createProgram();

        DataIntegrityViolationException databaseException =
                constraintViolation("fk_courses_program");

        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));
        when(courseRepository.existsByProgramId(programId))
                .thenReturn(false);
        when(studentRepository.existsByProgramId(programId))
                .thenReturn(false);
        doThrow(databaseException).when(programRepository).flush();

        assertThatThrownBy(() -> programService.deleteProgram(programId))
                .isInstanceOf(ProgramHasAssociatedCoursesException.class);
    }

    @Test
    void deleteProgram_throwsConflict_whenStudentConstraintRejectsDelete() {
        UUID programId = UUID.randomUUID();
        Program program = createProgram();

        DataIntegrityViolationException databaseException =
                constraintViolation("fk_students_program");

        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));
        when(courseRepository.existsByProgramId(programId))
                .thenReturn(false);
        when(studentRepository.existsByProgramId(programId))
                .thenReturn(false);
        doThrow(databaseException).when(programRepository).flush();

        assertThatThrownBy(() -> programService.deleteProgram(programId))
                .isInstanceOf(ProgramHasAssociatedStudentsException.class);
    }

    @Test
    void deleteProgram_rethrowsUnrelatedDatabaseError() {
        UUID programId = UUID.randomUUID();
        Program program = createProgram();

        DataIntegrityViolationException original =
                new DataIntegrityViolationException("other database error");

        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));
        when(courseRepository.existsByProgramId(programId))
                .thenReturn(false);
        when(studentRepository.existsByProgramId(programId))
                .thenReturn(false);
        doThrow(original).when(programRepository).flush();

        assertThatThrownBy(() -> programService.deleteProgram(programId))
                .isSameAs(original);
    }

    private Program createProgram() {
        Department department = mock(Department.class);
        return new Program("BSC-CS", "Computer Science", department);
    }

    private DataIntegrityViolationException constraintViolation(String name) {
        ConstraintViolationException cause =
                new ConstraintViolationException(
                        "foreign key violation",
                        new SQLException(),
                        name
                );

        return new DataIntegrityViolationException(
                "foreign key violation",
                cause
        );
    }
}
