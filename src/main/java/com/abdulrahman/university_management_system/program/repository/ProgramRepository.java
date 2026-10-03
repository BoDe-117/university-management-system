package com.abdulrahman.university_management_system.program.repository;

import com.abdulrahman.university_management_system.program.entity.Program;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProgramRepository extends JpaRepository<Program, UUID> {

    boolean existsByCode(String code);

    List<Program> findAllByDepartmentId(UUID departmentId);
}
