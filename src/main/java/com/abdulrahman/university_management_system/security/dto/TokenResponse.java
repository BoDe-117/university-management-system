package com.abdulrahman.university_management_system.security.dto;

public record TokenResponse(
        String accessToken,
        String tokenType
) {
}
