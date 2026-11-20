package com.abdulrahman.university_management_system.common.pagination;

public class InvalidPaginationException extends RuntimeException {

    public InvalidPaginationException(String message) {
        super(message);
    }
}
