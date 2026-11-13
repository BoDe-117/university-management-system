package com.abdulrahman.university_management_system.student.exception;

public class StudentIdAlreadyExistsException extends RuntimeException {

    public StudentIdAlreadyExistsException(String studentId) {
        super("A student with student ID '" + studentId + "' already exists");
    }
}
