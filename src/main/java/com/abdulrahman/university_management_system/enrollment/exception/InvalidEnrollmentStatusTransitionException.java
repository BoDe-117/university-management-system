package com.abdulrahman.university_management_system.enrollment.exception;

import java.util.UUID;

public class InvalidEnrollmentStatusTransitionException extends RuntimeException {

    public InvalidEnrollmentStatusTransitionException(UUID enrollmentId) {
        super("Cannot change the status of enrollment: " + enrollmentId);
    }
}
