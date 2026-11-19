package com.abdulrahman.university_management_system.enrollment.dto;

import com.abdulrahman.university_management_system.enrollment.entity.EnrollmentStatus;

import java.time.Instant;
import java.util.UUID;

public record EnrollmentResponse(
        UUID id,
        UUID studentId,
        UUID courseId,
        EnrollmentStatus status,
        Instant enrolledAt,
        Instant createdAt,
        Instant updatedAt
) {
}
