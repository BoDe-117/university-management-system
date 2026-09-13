package com.abdulrahman.university_management_system.department.exception;

import java.util.UUID;

public class DepartmentNotFoundException extends RuntimeException {

    public DepartmentNotFoundException(UUID id) {
        super("Department not found with id: " + id);
    }
}
