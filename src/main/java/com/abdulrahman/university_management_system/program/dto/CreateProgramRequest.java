package com.abdulrahman.university_management_system.program.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateProgramRequest(

        @NotBlank(message = "Program code is required")
        @Size(max = 20, message = "Program code must be at most 20 characters")
        String code,

        @NotBlank(message = "Program name is required")
        @Size(max = 150, message = "Program name must be at most 150 characters")
        String name,

        @NotNull(message = "Department ID is required")
        UUID departmentId

) {
}
