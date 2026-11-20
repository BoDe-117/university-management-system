package com.abdulrahman.university_management_system.student.exception;

import java.util.UUID;

public class StudentHasUserAccountException extends RuntimeException {

    public StudentHasUserAccountException(UUID studentId) {
        super("Cannot delete student " + studentId + " because they have a user account");
    }
}
