package com.abdulrahman.university_management_system.student.exception;

public class StudentEmailAlreadyExistsException extends RuntimeException {

    public StudentEmailAlreadyExistsException(String email) {
        super("A student with email '" + email + "' already exists");
    }
}
