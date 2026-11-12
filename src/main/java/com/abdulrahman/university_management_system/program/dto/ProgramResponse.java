package com.abdulrahman.university_management_system.program.dto;

import java.time.Instant;
import java.util.UUID;

public record ProgramResponse(
        UUID id,
        String code,
        String name,
        UUID departmentId,
        String departmentCode,
        String departmentName,
        Instant createdAt,
        Instant updatedAt
) {
}
