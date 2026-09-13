package com.abdulrahman.university_management_system.department.repository;

import com.abdulrahman.university_management_system.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    boolean existsByCode(String code);
}
