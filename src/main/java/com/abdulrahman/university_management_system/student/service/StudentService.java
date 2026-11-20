package com.abdulrahman.university_management_system.student.service;

import com.abdulrahman.university_management_system.common.pagination.PaginationValidator;
import com.abdulrahman.university_management_system.enrollment.repository.EnrollmentRepository;
import com.abdulrahman.university_management_system.program.entity.Program;
import com.abdulrahman.university_management_system.program.exception.ProgramNotFoundException;
import com.abdulrahman.university_management_system.program.repository.ProgramRepository;
import com.abdulrahman.university_management_system.student.dto.CreateStudentRequest;
import com.abdulrahman.university_management_system.student.dto.StudentResponse;
import com.abdulrahman.university_management_system.student.dto.UpdateStudentRequest;
import com.abdulrahman.university_management_system.student.entity.Student;
import com.abdulrahman.university_management_system.student.exception.StudentEmailAlreadyExistsException;
import com.abdulrahman.university_management_system.student.exception.StudentHasAssociatedEnrollmentsException;
import com.abdulrahman.university_management_system.student.exception.StudentHasUserAccountException;
import com.abdulrahman.university_management_system.student.exception.StudentIdAlreadyExistsException;
import com.abdulrahman.university_management_system.student.exception.StudentNotFoundException;
import com.abdulrahman.university_management_system.student.repository.StudentRepository;
import com.abdulrahman.university_management_system.user.repository.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class StudentService {

    private static final String STUDENT_ID_CONSTRAINT =
            "uk_students_student_id";

    private static final String STUDENT_EMAIL_CONSTRAINT =
            "uk_students_email";

    private static final String ENROLLMENT_STUDENT_CONSTRAINT =
            "fk_enrollments_student";

    private static final String USER_STUDENT_CONSTRAINT =
            "fk_users_student";

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "studentId", "firstName", "lastName",
            "email", "createdAt", "updatedAt"
    );

    private final StudentRepository studentRepository;
    private final ProgramRepository programRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final PaginationValidator paginationValidator;

    public StudentService(
            StudentRepository studentRepository,
            ProgramRepository programRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            PaginationValidator paginationValidator
    ) {
        this.studentRepository = studentRepository;
        this.programRepository = programRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.paginationValidator = paginationValidator;
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

        if (enrollmentRepository.existsByStudentId(id)) {
            throw new StudentHasAssociatedEnrollmentsException(id);
        }

        if (userRepository.existsByStudentId(id)) {
            throw new StudentHasUserAccountException(id);
        }

        try {
            studentRepository.delete(student);
            studentRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            if (hasConstraint(exception, ENROLLMENT_STUDENT_CONSTRAINT)) {
                throw new StudentHasAssociatedEnrollmentsException(id);
            }

            if (hasConstraint(exception, USER_STUDENT_CONSTRAINT)) {
                throw new StudentHasUserAccountException(id);
            }

            throw exception;
        }
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

    @Transactional(readOnly = true)
    public Page<StudentResponse> getStudents(
            UUID programId,
            Pageable pageable
    ) {
        Pageable validatedPageable =
                paginationValidator.validate(pageable, ALLOWED_SORT_FIELDS);

        if (programId == null) {
            return studentRepository.findAll(validatedPageable)
                    .map(this::toResponse);
        }

        if (!programRepository.existsById(programId)) {
            throw new ProgramNotFoundException(programId);
        }

        return studentRepository
                .findAllByProgramId(programId, validatedPageable)
                .map(this::toResponse);
    }

    private String getConstraintName(DataIntegrityViolationException exception) {
        if (exception.getCause()
                instanceof ConstraintViolationException constraintViolation) {
            return constraintViolation.getConstraintName();
        }

        return null;
    }

    private boolean hasConstraint(
            DataIntegrityViolationException exception,
            String constraintName
    ) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && constraintName.equals(
                    constraintViolation.getConstraintName()
            )) {
                return true;
            }

            if (cause instanceof PSQLException postgresException) {
                ServerErrorMessage details =
                        postgresException.getServerErrorMessage();

                if (details != null
                        && constraintName.equals(details.getConstraint())) {
                    return true;
                }
            }

            cause = cause.getCause();
        }

        return false;
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
