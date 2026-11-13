package com.abdulrahman.university_management_system.student.service;

import com.abdulrahman.university_management_system.program.entity.Program;
import com.abdulrahman.university_management_system.program.exception.ProgramNotFoundException;
import com.abdulrahman.university_management_system.program.repository.ProgramRepository;
import com.abdulrahman.university_management_system.student.dto.CreateStudentRequest;
import com.abdulrahman.university_management_system.student.dto.StudentResponse;
import com.abdulrahman.university_management_system.student.dto.UpdateStudentRequest;
import com.abdulrahman.university_management_system.student.entity.Student;
import com.abdulrahman.university_management_system.student.exception.StudentEmailAlreadyExistsException;
import com.abdulrahman.university_management_system.student.exception.StudentIdAlreadyExistsException;
import com.abdulrahman.university_management_system.student.exception.StudentNotFoundException;
import com.abdulrahman.university_management_system.student.repository.StudentRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class StudentService {

    private static final String STUDENT_ID_CONSTRAINT =
            "uk_students_student_id";

    private static final String STUDENT_EMAIL_CONSTRAINT =
            "uk_students_email";

    private final StudentRepository studentRepository;
    private final ProgramRepository programRepository;

    public StudentService(
            StudentRepository studentRepository,
            ProgramRepository programRepository
    ) {
        this.studentRepository = studentRepository;
        this.programRepository = programRepository;
    }

    @Transactional
    public StudentResponse createStudent(CreateStudentRequest request) {
        if (studentRepository.existsByStudentId(request.studentId())) {
            throw new StudentIdAlreadyExistsException(request.studentId());
        }

        if (studentRepository.existsByEmail(request.email())) {
            throw new StudentEmailAlreadyExistsException(request.email());
        }

        Program program = programRepository.findById(request.programId())
                .orElseThrow(() ->
                        new ProgramNotFoundException(request.programId())
                );

        Student student = new Student(
                request.studentId(),
                request.firstName(),
                request.lastName(),
                request.email(),
                program
        );

        try {
            Student saved = studentRepository.saveAndFlush(student);
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            String constraintName = getConstraintName(exception);

            if (STUDENT_ID_CONSTRAINT.equals(constraintName)) {
                throw new StudentIdAlreadyExistsException(request.studentId());
            }

            if (STUDENT_EMAIL_CONSTRAINT.equals(constraintName)) {
                throw new StudentEmailAlreadyExistsException(request.email());
            }

            throw exception;
        }
    }

    @Transactional
    public StudentResponse updateStudent(UUID id, UpdateStudentRequest request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException(id));

        if (studentRepository.existsByStudentIdAndIdNot(request.studentId(), id)) {
            throw new StudentIdAlreadyExistsException(request.studentId());
        }

        if (studentRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new StudentEmailAlreadyExistsException(request.email());
        }

        Program program = programRepository.findById(request.programId())
                .orElseThrow(() ->
                        new ProgramNotFoundException(request.programId())
                );

        student.setStudentId(request.studentId());
        student.setFirstName(request.firstName());
        student.setLastName(request.lastName());
        student.setEmail(request.email());
        student.setProgram(program);

        try {
            Student saved = studentRepository.saveAndFlush(student);
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            String constraintName = getConstraintName(exception);

            if (STUDENT_ID_CONSTRAINT.equals(constraintName)) {
                throw new StudentIdAlreadyExistsException(request.studentId());
            }

            if (STUDENT_EMAIL_CONSTRAINT.equals(constraintName)) {
                throw new StudentEmailAlreadyExistsException(request.email());
            }

            throw exception;
        }
    }

    @Transactional
    public void deleteStudent(UUID id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException(id));

        studentRepository.delete(student);
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudentById(UUID id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException(id));

        return toResponse(student);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getAllStudents() {
        return studentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getStudentsByProgramId(UUID programId) {
        if (!programRepository.existsById(programId)) {
            throw new ProgramNotFoundException(programId);
        }

        return studentRepository.findAllByProgramId(programId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private String getConstraintName(DataIntegrityViolationException exception) {
        if (exception.getCause()
                instanceof ConstraintViolationException constraintViolation) {
            return constraintViolation.getConstraintName();
        }

        return null;
    }

    private StudentResponse toResponse(Student student) {
        Program program = student.getProgram();

        return new StudentResponse(
                student.getId(),
                student.getStudentId(),
                student.getFirstName(),
                student.getLastName(),
                student.getEmail(),
                program.getId(),
                program.getCode(),
                program.getName(),
                student.getCreatedAt(),
                student.getUpdatedAt()
        );
    }
}
