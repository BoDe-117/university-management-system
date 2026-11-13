package com.abdulrahman.university_management_system.student.repository;

import com.abdulrahman.university_management_system.student.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {

    boolean existsByStudentId(String studentId);

    boolean existsByEmail(String email);

    boolean existsByStudentIdAndIdNot(String studentId, UUID id);

    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByProgramId(UUID programId);

    List<Student> findAllByProgramId(UUID programId);
}
