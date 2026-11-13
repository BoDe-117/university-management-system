package com.abdulrahman.university_management_system.student.service;

import com.abdulrahman.university_management_system.program.entity.Program;
import com.abdulrahman.university_management_system.program.exception.ProgramNotFoundException;
import com.abdulrahman.university_management_system.program.repository.ProgramRepository;
import com.abdulrahman.university_management_system.student.dto.StudentResponse;
import com.abdulrahman.university_management_system.student.dto.UpdateStudentRequest;
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
class StudentUpdateDeleteServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ProgramRepository programRepository;

    @InjectMocks
    private StudentService studentService;

    @Test
    void updateStudent_updatesFieldsAndMovesProgram() {
        UUID id = UUID.randomUUID();
        UUID programId = UUID.randomUUID();

        Program oldProgram = mock(Program.class);
        Program newProgram = mock(Program.class);
        Student student = student(oldProgram);
        UpdateStudentRequest request = request(programId);

        when(studentRepository.findById(id)).thenReturn(Optional.of(student));
        when(studentRepository.existsByStudentIdAndIdNot("STU-002", id))
                .thenReturn(false);
        when(studentRepository.existsByEmailAndIdNot("new@example.com", id))
                .thenReturn(false);
        when(programRepository.findById(programId))
                .thenReturn(Optional.of(newProgram));
        when(studentRepository.saveAndFlush(student)).thenReturn(student);

        when(newProgram.getId()).thenReturn(programId);
        when(newProgram.getCode()).thenReturn("BSC-SE");
        when(newProgram.getName()).thenReturn("Software Engineering");

        StudentResponse response = studentService.updateStudent(id, request);

        assertThat(response.studentId()).isEqualTo("STU-002");
        assertThat(response.firstName()).isEqualTo("New");
        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(response.programId()).isEqualTo(programId);
        assertThat(student.getProgram()).isSameAs(newProgram);

        verify(studentRepository).saveAndFlush(student);
    }

    @Test
    void updateStudent_rejectsMissingStudent() {
        UUID id = UUID.randomUUID();
        UpdateStudentRequest request = request(UUID.randomUUID());

        when(studentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.updateStudent(id, request))
                .isInstanceOf(StudentNotFoundException.class);

        verify(studentRepository, never()).saveAndFlush(any(Student.class));
    }

    @Test
    void updateStudent_rejectsStudentIdUsedByAnotherStudent() {
        UUID id = UUID.randomUUID();
        Student student = student(mock(Program.class));
        UpdateStudentRequest request = request(UUID.randomUUID());

        when(studentRepository.findById(id)).thenReturn(Optional.of(student));
        when(studentRepository.existsByStudentIdAndIdNot("STU-002", id))
                .thenReturn(true);

        assertThatThrownBy(() -> studentService.updateStudent(id, request))
                .isInstanceOf(StudentIdAlreadyExistsException.class);

        verify(studentRepository, never()).saveAndFlush(any(Student.class));
    }

    @Test
    void updateStudent_rejectsEmailUsedByAnotherStudent() {
        UUID id = UUID.randomUUID();
        Student student = student(mock(Program.class));
        UpdateStudentRequest request = request(UUID.randomUUID());

        when(studentRepository.findById(id)).thenReturn(Optional.of(student));
        when(studentRepository.existsByStudentIdAndIdNot("STU-002", id))
                .thenReturn(false);
        when(studentRepository.existsByEmailAndIdNot("new@example.com", id))
                .thenReturn(true);

        assertThatThrownBy(() -> studentService.updateStudent(id, request))
                .isInstanceOf(StudentEmailAlreadyExistsException.class);

        verify(studentRepository, never()).saveAndFlush(any(Student.class));
    }

    @Test
    void updateStudent_rejectsMissingProgram() {
        UUID id = UUID.randomUUID();
        UUID programId = UUID.randomUUID();
        Student student = student(mock(Program.class));
        UpdateStudentRequest request = request(programId);

        when(studentRepository.findById(id)).thenReturn(Optional.of(student));
        when(studentRepository.existsByStudentIdAndIdNot("STU-002", id))
                .thenReturn(false);
        when(studentRepository.existsByEmailAndIdNot("new@example.com", id))
                .thenReturn(false);
        when(programRepository.findById(programId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.updateStudent(id, request))
                .isInstanceOf(ProgramNotFoundException.class);

        verify(studentRepository, never()).saveAndFlush(any(Student.class));
    }

    @Test
    void updateStudent_translatesStudentIdConstraint() {
        UUID id = UUID.randomUUID();
        UUID programId = UUID.randomUUID();
        Program program = mock(Program.class);
        Student student = student(program);
        UpdateStudentRequest request = request(programId);

        when(studentRepository.findById(id)).thenReturn(Optional.of(student));
        when(studentRepository.existsByStudentIdAndIdNot("STU-002", id))
                .thenReturn(false);
        when(studentRepository.existsByEmailAndIdNot("new@example.com", id))
                .thenReturn(false);
        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));
        when(studentRepository.saveAndFlush(student))
                .thenThrow(constraintViolation("uk_students_student_id"));

        assertThatThrownBy(() -> studentService.updateStudent(id, request))
                .isInstanceOf(StudentIdAlreadyExistsException.class);
    }

    @Test
    void updateStudent_translatesEmailConstraint() {
        UUID id = UUID.randomUUID();
        UUID programId = UUID.randomUUID();
        Program program = mock(Program.class);
        Student student = student(program);
        UpdateStudentRequest request = request(programId);

        when(studentRepository.findById(id)).thenReturn(Optional.of(student));
        when(studentRepository.existsByStudentIdAndIdNot("STU-002", id))
                .thenReturn(false);
        when(studentRepository.existsByEmailAndIdNot("new@example.com", id))
                .thenReturn(false);
        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));
        when(studentRepository.saveAndFlush(student))
                .thenThrow(constraintViolation("uk_students_email"));

        assertThatThrownBy(() -> studentService.updateStudent(id, request))
                .isInstanceOf(StudentEmailAlreadyExistsException.class);
    }

    @Test
    void deleteStudent_deletesExistingStudent() {
        UUID id = UUID.randomUUID();
        Student student = student(mock(Program.class));

        when(studentRepository.findById(id)).thenReturn(Optional.of(student));

        studentService.deleteStudent(id);

        verify(studentRepository).delete(student);
    }

    @Test
    void deleteStudent_rejectsMissingStudent() {
        UUID id = UUID.randomUUID();

        when(studentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.deleteStudent(id))
                .isInstanceOf(StudentNotFoundException.class);

        verify(studentRepository, never()).delete(any(Student.class));
    }

    private Student student(Program program) {
        return new Student(
                "STU-001",
                "Sara",
                "Ahmed",
                "sara@example.com",
                program
        );
    }

    private UpdateStudentRequest request(UUID programId) {
        return new UpdateStudentRequest(
                "STU-002",
                "New",
                "Name",
                "new@example.com",
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
