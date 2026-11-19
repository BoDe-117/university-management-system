package com.abdulrahman.university_management_system.enrollment.exception;

import java.util.UUID;

public class EnrollmentAlreadyExistsException extends RuntimeException {

    public EnrollmentAlreadyExistsException(UUID studentId, UUID courseId) {
        super("Student " + studentId + " is already enrolled in course " + courseId);
    }
}
