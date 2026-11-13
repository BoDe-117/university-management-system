package com.abdulrahman.university_management_system.enrollment.exception;

import java.util.UUID;

public class EnrollmentNotFoundException extends RuntimeException {

    public EnrollmentNotFoundException(UUID id) {
        super("Enrollment not found with id: " + id);
    }
}
