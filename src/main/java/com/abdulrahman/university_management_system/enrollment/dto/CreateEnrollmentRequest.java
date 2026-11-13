package com.abdulrahman.university_management_system.enrollment.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateEnrollmentRequest(

        @NotNull(message = "Student ID is required")
        UUID studentId,

        @NotNull(message = "Course ID is required")
        UUID courseId

) {
}
