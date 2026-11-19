package com.abdulrahman.university_management_system.enrollment.repository;

import com.abdulrahman.university_management_system.enrollment.entity.Enrollment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    boolean existsByStudentIdAndCourseId(UUID studentId, UUID courseId);

    boolean existsByStudentId(UUID studentId);

    boolean existsByCourseId(UUID courseId);

    List<Enrollment> findAllByStudentId(UUID studentId);

    List<Enrollment> findAllByCourseId(UUID courseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Enrollment e WHERE e.id = :id")
    Optional<Enrollment> findByIdForUpdate(@Param("id") UUID id);
}
