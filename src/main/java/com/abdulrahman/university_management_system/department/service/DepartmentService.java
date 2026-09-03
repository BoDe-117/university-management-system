package com.abdulrahman.university_management_system.department.service;

import com.abdulrahman.university_management_system.department.dto.CreateDepartmentRequest;
import com.abdulrahman.university_management_system.department.dto.DepartmentResponse;
import com.abdulrahman.university_management_system.department.entity.Department;
import com.abdulrahman.university_management_system.department.exception.DepartmentCodeAlreadyExistsException;
import com.abdulrahman.university_management_system.department.exception.DepartmentNotFoundException;
import com.abdulrahman.university_management_system.department.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        if (departmentRepository.existsByCode(request.code())) {
            throw new DepartmentCodeAlreadyExistsException(request.code());
        }

        Department department = new Department(request.code(), request.name());
        Department saved = departmentRepository.save(department);
        return toResponse(saved);
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
