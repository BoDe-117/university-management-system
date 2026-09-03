package com.abdulrahman.university_management_system.department.dto;

import java.time.Instant;
import java.util.UUID;

public record DepartmentResponse(
        UUID id,
        String code,
        String name,
        Instant createdAt,
        Instant updatedAt
) {
}

