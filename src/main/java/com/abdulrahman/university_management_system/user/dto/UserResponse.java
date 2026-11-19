package com.abdulrahman.university_management_system.user.dto;

import com.abdulrahman.university_management_system.user.entity.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        UserRole role,
        UUID studentId,
        Instant createdAt,
        Instant updatedAt
) {
}
