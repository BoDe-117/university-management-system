package com.abdulrahman.university_management_system.program.repository;

import com.abdulrahman.university_management_system.program.entity.Program;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProgramRepository extends JpaRepository<Program, UUID> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);

    List<Program> findAllByDepartmentId(UUID departmentId);

    Page<Program> findAllByDepartmentId(UUID departmentId, Pageable pageable);

    boolean existsByDepartmentId(UUID departmentId);
}
