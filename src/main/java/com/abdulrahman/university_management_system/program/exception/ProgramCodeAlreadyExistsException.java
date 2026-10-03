package com.abdulrahman.university_management_system.program.exception;

public class ProgramCodeAlreadyExistsException extends RuntimeException {

    public ProgramCodeAlreadyExistsException(String code) {
        super("A program with code '" + code + "' already exists");
    }
}
