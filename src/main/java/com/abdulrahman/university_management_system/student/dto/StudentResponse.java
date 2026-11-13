package com.abdulrahman.university_management_system.student.dto;

import java.time.Instant;
import java.util.UUID;

public record StudentResponse(
        UUID id,
        String studentId,
        String firstName,
        String lastName,
        String email,
        UUID programId,
        String programCode,
        String programName,
        Instant createdAt,
        Instant updatedAt
) {
}
