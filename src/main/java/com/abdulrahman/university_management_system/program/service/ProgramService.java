package com.abdulrahman.university_management_system.program.service;

import com.abdulrahman.university_management_system.course.repository.CourseRepository;
import com.abdulrahman.university_management_system.department.entity.Department;
import com.abdulrahman.university_management_system.department.exception.DepartmentNotFoundException;
import com.abdulrahman.university_management_system.department.repository.DepartmentRepository;
import com.abdulrahman.university_management_system.program.dto.CreateProgramRequest;
import com.abdulrahman.university_management_system.program.dto.ProgramResponse;
import com.abdulrahman.university_management_system.program.dto.UpdateProgramRequest;
import com.abdulrahman.university_management_system.program.entity.Program;
import com.abdulrahman.university_management_system.program.exception.ProgramCodeAlreadyExistsException;
import com.abdulrahman.university_management_system.program.exception.ProgramHasAssociatedCoursesException;
import com.abdulrahman.university_management_system.program.exception.ProgramHasAssociatedStudentsException;
import com.abdulrahman.university_management_system.program.exception.ProgramNotFoundException;
import com.abdulrahman.university_management_system.program.repository.ProgramRepository;
import com.abdulrahman.university_management_system.student.repository.StudentRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProgramService {

    private static final String PROGRAM_CODE_UNIQUE_CONSTRAINT =
            "uk_programs_code";

    private static final String COURSE_PROGRAM_FOREIGN_KEY =
            "fk_courses_program";

    private static final String STUDENT_PROGRAM_FOREIGN_KEY =
            "fk_students_program";

    private final ProgramRepository programRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;

    public ProgramService(
            ProgramRepository programRepository,
            DepartmentRepository departmentRepository,
            CourseRepository courseRepository,
            StudentRepository studentRepository
    ) {
        this.programRepository = programRepository;
        this.departmentRepository = departmentRepository;
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public ProgramResponse createProgram(CreateProgramRequest request) {
        if (programRepository.existsByCode(request.code())) {
            throw new ProgramCodeAlreadyExistsException(request.code());
        }

        Department department = departmentRepository
                .findById(request.departmentId())
                .orElseThrow(() ->
                        new DepartmentNotFoundException(request.departmentId())
                );

        Program program = new Program(
                request.code(),
                request.name(),
                department
        );

        try {
            Program saved = programRepository.saveAndFlush(program);
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateCodeViolation(exception)) {
                throw new ProgramCodeAlreadyExistsException(request.code());
            }

            throw exception;
        }
    }

    @Transactional
    public ProgramResponse updateProgram(
            UUID id,
            UpdateProgramRequest request
    ) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new ProgramNotFoundException(id));

        if (programRepository.existsByCodeAndIdNot(request.code(), id)) {
            throw new ProgramCodeAlreadyExistsException(request.code());
        }

        Department department = departmentRepository
                .findById(request.departmentId())
                .orElseThrow(() ->
                        new DepartmentNotFoundException(request.departmentId())
                );

        program.setCode(request.code());
        program.setName(request.name());
        program.setDepartment(department);

        try {
            Program saved = programRepository.saveAndFlush(program);
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateCodeViolation(exception)) {
                throw new ProgramCodeAlreadyExistsException(request.code());
            }

            throw exception;
        }
    }

    @Transactional
    public void deleteProgram(UUID id) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new ProgramNotFoundException(id));

        if (courseRepository.existsByProgramId(id)) {
            throw new ProgramHasAssociatedCoursesException(program.getCode());
        }

        if (studentRepository.existsByProgramId(id)) {
            throw new ProgramHasAssociatedStudentsException(program.getCode());
        }

        try {
            programRepository.delete(program);
            programRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            if (hasConstraint(exception, COURSE_PROGRAM_FOREIGN_KEY)) {
                throw new ProgramHasAssociatedCoursesException(program.getCode());
            }

            if (hasConstraint(exception, STUDENT_PROGRAM_FOREIGN_KEY)) {
                throw new ProgramHasAssociatedStudentsException(program.getCode());
            }

            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public ProgramResponse getProgramById(UUID id) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new ProgramNotFoundException(id));

        return toResponse(program);
    }

    @Transactional(readOnly = true)
    public List<ProgramResponse> getAllPrograms() {
        return programRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProgramResponse> getProgramsByDepartmentId(UUID departmentId) {
        if (!departmentRepository.existsById(departmentId)) {
            throw new DepartmentNotFoundException(departmentId);
        }

        return programRepository.findAllByDepartmentId(departmentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private boolean isDuplicateCodeViolation(
            DataIntegrityViolationException exception
    ) {
        if (exception.getCause()
                instanceof ConstraintViolationException constraintViolation) {

            return PROGRAM_CODE_UNIQUE_CONSTRAINT.equals(
                    constraintViolation.getConstraintName()
            );
        }

        return false;
    }

    private boolean hasConstraint(
            DataIntegrityViolationException exception,
            String constraintName
    ) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && constraintName.equals(constraintViolation.getConstraintName())) {
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

    private ProgramResponse toResponse(Program program) {
        Department department = program.getDepartment();

        return new ProgramResponse(
                program.getId(),
                program.getCode(),
                program.getName(),
                department.getId(),
                department.getCode(),
                department.getName(),
                program.getCreatedAt(),
                program.getUpdatedAt()
        );
    }
}
