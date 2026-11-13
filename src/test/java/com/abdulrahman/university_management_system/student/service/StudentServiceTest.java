package com.abdulrahman.university_management_system.student.service;

import com.abdulrahman.university_management_system.program.entity.Program;
import com.abdulrahman.university_management_system.program.exception.ProgramNotFoundException;
import com.abdulrahman.university_management_system.program.repository.ProgramRepository;
import com.abdulrahman.university_management_system.student.dto.CreateStudentRequest;
import com.abdulrahman.university_management_system.student.dto.StudentResponse;
import com.abdulrahman.university_management_system.student.entity.Student;
import com.abdulrahman.university_management_system.student.exception.StudentEmailAlreadyExistsException;
import com.abdulrahman.university_management_system.student.exception.StudentIdAlreadyExistsException;
import com.abdulrahman.university_management_system.student.exception.StudentNotFoundException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ProgramRepository programRepository;

    @InjectMocks
    private StudentService studentService;

    @Test
    void createStudent_savesAndReturnsStudent() {
        UUID programId = UUID.randomUUID();
        Program program = mock(Program.class);
        CreateStudentRequest request = request(programId);

        Student saved = new Student(
                request.studentId(),
                request.firstName(),
                request.lastName(),
                request.email(),
                program
        );

        when(studentRepository.existsByStudentId("STU-001")).thenReturn(false);
        when(studentRepository.existsByEmail("sara@example.com")).thenReturn(false);
        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));
        when(studentRepository.saveAndFlush(any(Student.class)))
                .thenReturn(saved);

        when(program.getId()).thenReturn(programId);
        when(program.getCode()).thenReturn("BSC-CS");
        when(program.getName()).thenReturn("Computer Science");

        StudentResponse response = studentService.createStudent(request);

        assertThat(response.studentId()).isEqualTo("STU-001");
        assertThat(response.firstName()).isEqualTo("Sara");
        assertThat(response.lastName()).isEqualTo("Ahmed");
        assertThat(response.email()).isEqualTo("sara@example.com");
        assertThat(response.programId()).isEqualTo(programId);
        assertThat(response.programCode()).isEqualTo("BSC-CS");

        verify(studentRepository).saveAndFlush(any(Student.class));
    }

    @Test
    void createStudent_rejectsDuplicateStudentId() {
        CreateStudentRequest request = request(UUID.randomUUID());

        when(studentRepository.existsByStudentId("STU-001")).thenReturn(true);

        assertThatThrownBy(() -> studentService.createStudent(request))
                .isInstanceOf(StudentIdAlreadyExistsException.class);

        verify(studentRepository, never()).saveAndFlush(any(Student.class));
    }

    @Test
    void createStudent_rejectsDuplicateEmail() {
        CreateStudentRequest request = request(UUID.randomUUID());

        when(studentRepository.existsByStudentId("STU-001")).thenReturn(false);
        when(studentRepository.existsByEmail("sara@example.com")).thenReturn(true);

        assertThatThrownBy(() -> studentService.createStudent(request))
                .isInstanceOf(StudentEmailAlreadyExistsException.class);

        verify(studentRepository, never()).saveAndFlush(any(Student.class));
    }

    @Test
    void createStudent_rejectsMissingProgram() {
        UUID programId = UUID.randomUUID();
        CreateStudentRequest request = request(programId);

        when(studentRepository.existsByStudentId("STU-001")).thenReturn(false);
        when(studentRepository.existsByEmail("sara@example.com")).thenReturn(false);
        when(programRepository.findById(programId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.createStudent(request))
                .isInstanceOf(ProgramNotFoundException.class);

        verify(studentRepository, never()).saveAndFlush(any(Student.class));
    }

    @Test
    void createStudent_translatesStudentIdConstraint() {
        UUID programId = UUID.randomUUID();
        CreateStudentRequest request = request(programId);
        Program program = mock(Program.class);

        when(studentRepository.existsByStudentId("STU-001")).thenReturn(false);
        when(studentRepository.existsByEmail("sara@example.com")).thenReturn(false);
        when(programRepository.findById(programId)).thenReturn(Optional.of(program));
        when(studentRepository.saveAndFlush(any(Student.class)))
                .thenThrow(constraintViolation("uk_students_student_id"));

        assertThatThrownBy(() -> studentService.createStudent(request))
                .isInstanceOf(StudentIdAlreadyExistsException.class);
    }

    @Test
    void createStudent_translatesEmailConstraint() {
        UUID programId = UUID.randomUUID();
        CreateStudentRequest request = request(programId);
        Program program = mock(Program.class);

        when(studentRepository.existsByStudentId("STU-001")).thenReturn(false);
        when(studentRepository.existsByEmail("sara@example.com")).thenReturn(false);
        when(programRepository.findById(programId)).thenReturn(Optional.of(program));
        when(studentRepository.saveAndFlush(any(Student.class)))
                .thenThrow(constraintViolation("uk_students_email"));

        assertThatThrownBy(() -> studentService.createStudent(request))
                .isInstanceOf(StudentEmailAlreadyExistsException.class);
    }

    @Test
    void createStudent_rethrowsUnrelatedDatabaseError() {
        UUID programId = UUID.randomUUID();
        CreateStudentRequest request = request(programId);
        Program program = mock(Program.class);

        DataIntegrityViolationException original =
                new DataIntegrityViolationException("other database error");

        when(studentRepository.existsByStudentId("STU-001")).thenReturn(false);
        when(studentRepository.existsByEmail("sara@example.com")).thenReturn(false);
        when(programRepository.findById(programId)).thenReturn(Optional.of(program));
        when(studentRepository.saveAndFlush(any(Student.class))).thenThrow(original);

        assertThatThrownBy(() -> studentService.createStudent(request))
                .isSameAs(original);
    }

    @Test
    void getStudentById_rejectsMissingStudent() {
        UUID studentId = UUID.randomUUID();

        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getStudentById(studentId))
                .isInstanceOf(StudentNotFoundException.class);
    }

    @Test
    void getStudentsByProgramId_rejectsMissingProgram() {
        UUID programId = UUID.randomUUID();

        when(programRepository.existsById(programId)).thenReturn(false);

        assertThatThrownBy(() -> studentService.getStudentsByProgramId(programId))
                .isInstanceOf(ProgramNotFoundException.class);

        verify(studentRepository, never()).findAllByProgramId(programId);
    }

    private CreateStudentRequest request(UUID programId) {
        return new CreateStudentRequest(
                "STU-001",
                "Sara",
                "Ahmed",
                "sara@example.com",
                programId
        );
    }

    private DataIntegrityViolationException constraintViolation(String name) {
        ConstraintViolationException cause =
                new ConstraintViolationException(
                        "unique constraint violation",
                        new SQLException(),
                        name
                );

        return new DataIntegrityViolationException(
                "unique constraint violation",
                cause
        );
    }
}
