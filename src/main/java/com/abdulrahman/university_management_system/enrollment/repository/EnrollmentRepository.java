package com.abdulrahman.university_management_system.enrollment.repository;

import com.abdulrahman.university_management_system.enrollment.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    boolean existsByStudentIdAndCourseId(UUID studentId, UUID courseId);

    boolean existsByStudentId(UUID studentId);

    boolean existsByCourseId(UUID courseId);

    List<Enrollment> findAllByStudentId(UUID studentId);

    List<Enrollment> findAllByCourseId(UUID courseId);
}
