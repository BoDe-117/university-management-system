package com.abdulrahman.university_management_system.course.exception;

public class CourseCodeAlreadyExistsException extends RuntimeException {

    public CourseCodeAlreadyExistsException(String code) {
        super("A course with code '" + code + "' already exists");
    }
}
