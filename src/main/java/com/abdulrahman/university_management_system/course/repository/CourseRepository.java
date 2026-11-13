package com.abdulrahman.university_management_system.course.repository;

import com.abdulrahman.university_management_system.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);

    List<Course> findAllByProgramId(UUID programId);

    boolean existsByProgramId(UUID programId);
}
