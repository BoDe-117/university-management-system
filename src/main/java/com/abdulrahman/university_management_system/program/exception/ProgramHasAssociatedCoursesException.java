package com.abdulrahman.university_management_system.program.exception;

public class ProgramHasAssociatedCoursesException extends RuntimeException {

    public ProgramHasAssociatedCoursesException(String programCode) {
        super(
                "Cannot delete program '" + programCode
                        + "' because courses are assigned to it"
        );
    }
}
