package com.abdulrahman.university_management_system.department.exception;

public class DepartmentHasAssociatedProgramsException extends RuntimeException {

    public DepartmentHasAssociatedProgramsException(String departmentCode) {
        super(
                "Cannot delete department '" + departmentCode
                        + "' because programs are assigned to it"
        );
    }
}
