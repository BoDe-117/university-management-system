package com.abdulrahman.university_management_system.program.exception;

public class ProgramHasAssociatedStudentsException extends RuntimeException {

    public ProgramHasAssociatedStudentsException(String programCode) {
        super(
                "Cannot delete program '" + programCode
                        + "' because students are assigned to it"
        );
    }
}
