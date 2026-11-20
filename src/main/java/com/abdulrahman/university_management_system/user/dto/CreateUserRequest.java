package com.abdulrahman.university_management_system.user.dto;

import com.abdulrahman.university_management_system.user.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateUserRequest(

        @NotBlank(message = "Username is required")
        @Size(max = 50, message = "Username must not exceed 50 characters")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 12, message = "Password must be at least 12 characters")
        String password,

        @NotNull(message = "Role is required")
        UserRole role,

        UUID studentId

) {
}
