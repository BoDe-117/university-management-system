package com.abdulrahman.university_management_system.department.service;

import com.abdulrahman.university_management_system.department.dto.DepartmentResponse;
import com.abdulrahman.university_management_system.department.dto.UpdateDepartmentRequest;
import com.abdulrahman.university_management_system.department.entity.Department;
import com.abdulrahman.university_management_system.department.exception.DepartmentCodeAlreadyExistsException;
import com.abdulrahman.university_management_system.department.exception.DepartmentNotFoundException;
import com.abdulrahman.university_management_system.department.repository.DepartmentRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentUpdateServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private DepartmentService departmentService;

    @Test
    void updateDepartment_updatesCodeAndName() {
        UUID id = UUID.randomUUID();
        Department department = new Department("CS", "Computer Science");
        UpdateDepartmentRequest request =
                new UpdateDepartmentRequest("SE", "Software Engineering");

        when(departmentRepository.findById(id))
                .thenReturn(Optional.of(department));
        when(departmentRepository.existsByCodeAndIdNot("SE", id))
                .thenReturn(false);
        when(departmentRepository.saveAndFlush(department))
                .thenReturn(department);

        DepartmentResponse response = departmentService.updateDepartment(id, request);

        assertThat(response.code()).isEqualTo("SE");
        assertThat(response.name()).isEqualTo("Software Engineering");
        verify(departmentRepository).saveAndFlush(department);
    }

    @Test
    void updateDepartment_allowsKeepingTheSameCode() {
        UUID id = UUID.randomUUID();
        Department department = new Department("CS", "Old Name");
        UpdateDepartmentRequest request =
                new UpdateDepartmentRequest("CS", "New Name");

        when(departmentRepository.findById(id))
                .thenReturn(Optional.of(department));
        when(departmentRepository.existsByCodeAndIdNot("CS", id))
                .thenReturn(false);
        when(departmentRepository.saveAndFlush(department))
                .thenReturn(department);

        DepartmentResponse response = departmentService.updateDepartment(id, request);

        assertThat(response.code()).isEqualTo("CS");
        assertThat(response.name()).isEqualTo("New Name");
        verify(departmentRepository).existsByCodeAndIdNot("CS", id);
    }

    @Test
    void updateDepartment_throwsNotFound_whenDepartmentDoesNotExist() {
        UUID id = UUID.randomUUID();
        UpdateDepartmentRequest request =
                new UpdateDepartmentRequest("CS", "Computer Science");

        when(departmentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.updateDepartment(id, request))
                .isInstanceOf(DepartmentNotFoundException.class);

        verify(departmentRepository, never()).saveAndFlush(any(Department.class));
    }

    @Test
    void updateDepartment_throwsConflict_whenAnotherDepartmentUsesCode() {
        UUID id = UUID.randomUUID();
        Department department = new Department("CS", "Computer Science");
        UpdateDepartmentRequest request =
                new UpdateDepartmentRequest("SE", "Software Engineering");

        when(departmentRepository.findById(id))
                .thenReturn(Optional.of(department));
        when(departmentRepository.existsByCodeAndIdNot("SE", id))
                .thenReturn(true);

        assertThatThrownBy(() -> departmentService.updateDepartment(id, request))
                .isInstanceOf(DepartmentCodeAlreadyExistsException.class)
                .hasMessageContaining("SE");

        verify(departmentRepository, never()).saveAndFlush(any(Department.class));
    }

    @Test
    void updateDepartment_throwsConflict_whenDatabaseRejectsDuplicateCode() {
        UUID id = UUID.randomUUID();
        Department department = new Department("CS", "Computer Science");
        UpdateDepartmentRequest request =
                new UpdateDepartmentRequest("SE", "Software Engineering");

        ConstraintViolationException constraintException =
                new ConstraintViolationException(
                        "duplicate key",
                        new SQLException(),
                        "departments_code_key"
                );

        when(departmentRepository.findById(id))
                .thenReturn(Optional.of(department));
        when(departmentRepository.existsByCodeAndIdNot("SE", id))
                .thenReturn(false);
        when(departmentRepository.saveAndFlush(department))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicate key",
                        constraintException
                ));

        assertThatThrownBy(() -> departmentService.updateDepartment(id, request))
                .isInstanceOf(DepartmentCodeAlreadyExistsException.class)
                .hasMessageContaining("SE");
    }

    @Test
    void updateDepartment_rethrowsUnrelatedDatabaseError() {
        UUID id = UUID.randomUUID();
        Department department = new Department("CS", "Computer Science");
        UpdateDepartmentRequest request =
                new UpdateDepartmentRequest("SE", "Software Engineering");

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
        when(departmentRepository.existsByCodeAndIdNot("SE", id))
                .thenReturn(false);
        when(departmentRepository.saveAndFlush(department))
                .thenThrow(original);

        assertThatThrownBy(() -> departmentService.updateDepartment(id, request))
                .isSameAs(original);
    }
}
