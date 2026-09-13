package com.abdulrahman.university_management_system.department.exception;

public class DepartmentCodeAlreadyExistsException extends RuntimeException {

    public DepartmentCodeAlreadyExistsException(String code) {
        super("A department with code '" + code + "' already exists");
    }
}
