package com.abdulrahman.university_management_system.course.dto;

import java.time.Instant;
import java.util.UUID;

public record CourseResponse(
        UUID id,
        String code,
        String name,
        int credits,
        UUID programId,
        String programCode,
        String programName,
        Instant createdAt,
        Instant updatedAt
) {
}
