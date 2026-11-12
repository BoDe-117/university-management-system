package com.abdulrahman.university_management_system.program.exception;

import java.util.UUID;

public class ProgramNotFoundException extends RuntimeException {

    public ProgramNotFoundException(UUID id) {
        super("Program not found with id: " + id);
    }
}
