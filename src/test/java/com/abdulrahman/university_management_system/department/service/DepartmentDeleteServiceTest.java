package com.abdulrahman.university_management_system.department.service;

import com.abdulrahman.university_management_system.department.entity.Department;
import com.abdulrahman.university_management_system.department.exception.DepartmentHasAssociatedProgramsException;
import com.abdulrahman.university_management_system.department.exception.DepartmentNotFoundException;
import com.abdulrahman.university_management_system.department.repository.DepartmentRepository;
import com.abdulrahman.university_management_system.program.repository.ProgramRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentDeleteServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private ProgramRepository programRepository;

    @InjectMocks
    private DepartmentService departmentService;

    @Test
    void deleteDepartment_deletesDepartment_whenNoProgramsExist() {
        UUID id = UUID.randomUUID();
        Department department = new Department("CS", "Computer Science");

        when(departmentRepository.findById(id))
                .thenReturn(Optional.of(department));
        when(programRepository.existsByDepartmentId(id))
                .thenReturn(false);

        departmentService.deleteDepartment(id);

        verify(departmentRepository).delete(department);
        verify(departmentRepository).flush();
    }

    @Test
    void deleteDepartment_throwsNotFound_whenDepartmentDoesNotExist() {
        UUID id = UUID.randomUUID();

        when(departmentRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.deleteDepartment(id))
                .isInstanceOf(DepartmentNotFoundException.class);

        verify(programRepository, never()).existsByDepartmentId(id);
    }

    @Test
    void deleteDepartment_throwsConflict_whenProgramsExist() {
        UUID id = UUID.randomUUID();
        Department department = new Department("CS", "Computer Science");

        when(departmentRepository.findById(id))
                .thenReturn(Optional.of(department));
        when(programRepository.existsByDepartmentId(id))
                .thenReturn(true);

        assertThatThrownBy(() -> departmentService.deleteDepartment(id))
                .isInstanceOf(DepartmentHasAssociatedProgramsException.class)
                .hasMessageContaining("CS");

        verify(departmentRepository, never()).delete(department);
        verify(departmentRepository, never()).flush();
    }

    @Test
    void deleteDepartment_throwsConflict_whenDatabaseRejectsDelete() {
        UUID id = UUID.randomUUID();
        Department department = new Department("CS", "Computer Science");

        ConstraintViolationException constraintException =
                new ConstraintViolationException(
                        "foreign key violation",
                        new SQLException(),
                        "fk_programs_department"
                );

        DataIntegrityViolationException databaseException =
                new DataIntegrityViolationException(
                        "foreign key violation",
                        constraintException
                );

        when(departmentRepository.findById(id))
                .thenReturn(Optional.of(department));
        when(programRepository.existsByDepartmentId(id))
                .thenReturn(false);
        doThrow(databaseException).when(departmentRepository).flush();

        assertThatThrownBy(() -> departmentService.deleteDepartment(id))
                .isInstanceOf(DepartmentHasAssociatedProgramsException.class)
                .hasMessageContaining("CS");

        verify(departmentRepository).delete(department);
        verify(departmentRepository).flush();
    }

    @Test
    void deleteDepartment_rethrowsUnrelatedDatabaseError() {
        UUID id = UUID.randomUUID();
        Department department = new Department("CS", "Computer Science");

        ConstraintViolationException constraintException =
                new ConstraintViolationException(
                        "different constraint",
                        new SQLException(),
                        "another_constraint"
                );

        DataIntegrityViolationException original =
                new DataIntegrityViolationException(
                        "database error",
                        constraintException
                );

        when(departmentRepository.findById(id))
                .thenReturn(Optional.of(department));
        when(programRepository.existsByDepartmentId(id))
                .thenReturn(false);
        doThrow(original).when(departmentRepository).flush();

        assertThatThrownBy(() -> departmentService.deleteDepartment(id))
                .isSameAs(original);
    }
}
