package com.abdulrahman.university_management_system.student.exception;

import java.util.UUID;

public class StudentHasAssociatedEnrollmentsException extends RuntimeException {

    public StudentHasAssociatedEnrollmentsException(UUID studentId) {
        super("Cannot delete student " + studentId + " because they have associated enrollments");
    }
}
