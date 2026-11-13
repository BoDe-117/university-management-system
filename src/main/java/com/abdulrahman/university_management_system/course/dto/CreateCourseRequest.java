package com.abdulrahman.university_management_system.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCourseRequest(

        @NotBlank(message = "Course code is required")
        @Size(max = 20, message = "Course code must be at most 20 characters")
        String code,

        @NotBlank(message = "Course name is required")
        @Size(max = 150, message = "Course name must be at most 150 characters")
        String name,

        @NotNull(message = "Course credits are required")
        @Min(value = 1, message = "Course credits must be at least 1")
        @Max(value = 12, message = "Course credits must be at most 12")
        Integer credits,

        @NotNull(message = "Program ID is required")
        UUID programId

) {
}
