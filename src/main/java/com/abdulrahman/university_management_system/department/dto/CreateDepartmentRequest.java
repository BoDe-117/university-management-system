package com.abdulrahman.university_management_system.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDepartmentRequest(

        @NotBlank(message = "Department code is required")
        @Size(max = 20, message = "Department code must be at most 20 characters")
        String code,

        @NotBlank(message = "Department name is required")
        @Size(max = 150, message = "Department name must be at most 150 characters")
        String name

) {
}
