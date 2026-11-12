package com.abdulrahman.university_management_system.department.service;

import com.abdulrahman.university_management_system.department.dto.CreateDepartmentRequest;
import com.abdulrahman.university_management_system.department.dto.DepartmentResponse;
import com.abdulrahman.university_management_system.department.dto.UpdateDepartmentRequest;
import com.abdulrahman.university_management_system.department.entity.Department;
import com.abdulrahman.university_management_system.department.exception.DepartmentCodeAlreadyExistsException;
import com.abdulrahman.university_management_system.department.exception.DepartmentHasAssociatedProgramsException;
import com.abdulrahman.university_management_system.department.exception.DepartmentNotFoundException;
import com.abdulrahman.university_management_system.department.repository.DepartmentRepository;
import com.abdulrahman.university_management_system.program.repository.ProgramRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DepartmentService {

    private static final String DEPARTMENT_CODE_UNIQUE_CONSTRAINT =
            "departments_code_key";

    private static final String PROGRAM_DEPARTMENT_FOREIGN_KEY =
            "fk_programs_department";

    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;

    public DepartmentService(
            DepartmentRepository departmentRepository,
            ProgramRepository programRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.programRepository = programRepository;
    }

    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        if (departmentRepository.existsByCode(request.code())) {
            throw new DepartmentCodeAlreadyExistsException(request.code());
        }

        Department department = new Department(request.code(), request.name());

        try {
            Department saved = departmentRepository.saveAndFlush(department);
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateCodeViolation(e)) {
                throw new DepartmentCodeAlreadyExistsException(request.code());
            }
            throw e;
        }
    }

    @Transactional
    public DepartmentResponse updateDepartment(
            UUID id,
            UpdateDepartmentRequest request
    ) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id));

        if (departmentRepository.existsByCodeAndIdNot(request.code(), id)) {
            throw new DepartmentCodeAlreadyExistsException(request.code());
        }

        department.setCode(request.code());
        department.setName(request.name());

        try {
            Department saved = departmentRepository.saveAndFlush(department);
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateCodeViolation(e)) {
                throw new DepartmentCodeAlreadyExistsException(request.code());
            }
            throw e;
        }
    }

    @Transactional
    public void deleteDepartment(UUID id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id));

        if (programRepository.existsByDepartmentId(id)) {
            throw new DepartmentHasAssociatedProgramsException(
                    department.getCode()
            );
        }

        try {
            departmentRepository.delete(department);
            departmentRepository.flush();
        } catch (DataIntegrityViolationException e) {
            if (isProgramDepartmentViolation(e)) {
                throw new DepartmentHasAssociatedProgramsException(
                        department.getCode()
                );
            }
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(UUID id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new DepartmentNotFoundException(id));

        return toResponse(department);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private boolean isDuplicateCodeViolation(DataIntegrityViolationException e) {
        if (e.getCause() instanceof ConstraintViolationException cve) {
            return DEPARTMENT_CODE_UNIQUE_CONSTRAINT.equals(
                    cve.getConstraintName()
            );
        }
        return false;
    }

    private boolean isProgramDepartmentViolation(
            DataIntegrityViolationException exception
    ) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException cve
                    && PROGRAM_DEPARTMENT_FOREIGN_KEY.equals(
                    cve.getConstraintName()
            )) {
                return true;
            }

            if (cause instanceof PSQLException postgresException) {
                ServerErrorMessage details =
                        postgresException.getServerErrorMessage();

                if (details != null
                        && PROGRAM_DEPARTMENT_FOREIGN_KEY.equals(
                        details.getConstraint()
                )) {
                    return true;
                }
            }

            cause = cause.getCause();
        }

        return false;
    }

    private DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getCode(),
                department.getName(),
                department.getCreatedAt(),
                department.getUpdatedAt()
        );
    }
}

