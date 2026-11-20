package com.abdulrahman.university_management_system.user.exception;

import java.util.UUID;

public class StudentAlreadyHasAccountException extends RuntimeException {

    public StudentAlreadyHasAccountException(UUID studentId) {
        super("Student already has a user account: " + studentId);
    }
}
