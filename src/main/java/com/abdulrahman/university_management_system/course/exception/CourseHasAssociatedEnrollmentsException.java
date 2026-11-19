package com.abdulrahman.university_management_system.course.exception;

import java.util.UUID;

public class CourseHasAssociatedEnrollmentsException extends RuntimeException {

    public CourseHasAssociatedEnrollmentsException(UUID courseId) {
        super("Cannot delete course " + courseId + " because it has associated enrollments");
    }
}
