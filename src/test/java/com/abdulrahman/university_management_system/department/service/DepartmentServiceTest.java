package com.abdulrahman.university_management_system.department.service;

import com.abdulrahman.university_management_system.department.dto.CreateDepartmentRequest;
import com.abdulrahman.university_management_system.department.dto.DepartmentResponse;
import com.abdulrahman.university_management_system.department.entity.Department;
import com.abdulrahman.university_management_system.department.exception.DepartmentCodeAlreadyExistsException;
import com.abdulrahman.university_management_system.department.exception.DepartmentNotFoundException;
import com.abdulrahman.university_management_system.department.repository.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private DepartmentService departmentService;

    @Test
    void createDepartment_savesAndReturnsResponse_whenCodeIsUnique() {
        CreateDepartmentRequest request = new CreateDepartmentRequest("CS", "Computer Science");
        Department saved = new Department("CS", "Computer Science");
        setId(saved, UUID.randomUUID());
        setTimestamps(saved, Instant.now(), Instant.now());

        when(departmentRepository.existsByCode("CS")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(saved);

        DepartmentResponse response = departmentService.createDepartment(request);

        assertThat(response.code()).isEqualTo("CS");
        assertThat(response.name()).isEqualTo("Computer Science");
        assertThat(response.id()).isNotNull();
        verify(departmentRepository).save(any(Department.class));
    }

    @Test
    void createDepartment_throwsConflict_whenCodeAlreadyExists() {
        CreateDepartmentRequest request = new CreateDepartmentRequest("CS", "Computer Science");
        when(departmentRepository.existsByCode("CS")).thenReturn(true);

        assertThatThrownBy(() -> departmentService.createDepartment(request))
                .isInstanceOf(DepartmentCodeAlreadyExistsException.class)
                .hasMessageContaining("CS");

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void getDepartmentById_returnsResponse_whenDepartmentExists() {
        UUID id = UUID.randomUUID();
        Department department = new Department("SE", "Software Engineering");
        setId(department, id);
        setTimestamps(department, Instant.now(), Instant.now());

        when(departmentRepository.findById(id)).thenReturn(Optional.of(department));

        DepartmentResponse response = departmentService.getDepartmentById(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.code()).isEqualTo("SE");
    }

    @Test
    void getDepartmentById_throwsNotFound_whenDepartmentDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(departmentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.getDepartmentById(id))
                .isInstanceOf(DepartmentNotFoundException.class);
    }

    @Test
    void getAllDepartments_returnsMappedList() {
        Department cs = new Department("CS", "Computer Science");
        setId(cs, UUID.randomUUID());
        setTimestamps(cs, Instant.now(), Instant.now());

        Department se = new Department("SE", "Software Engineering");
        setId(se, UUID.randomUUID());
        setTimestamps(se, Instant.now(), Instant.now());

        when(departmentRepository.findAll()).thenReturn(List.of(cs, se));

        List<DepartmentResponse> responses = departmentService.getAllDepartments();

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(DepartmentResponse::code)
                .containsExactlyInAnyOrder("CS", "SE");
    }

    @Test
    void createDepartment_throwsConflict_whenDatabaseRejectsDuplicateCode() {
        CreateDepartmentRequest request = new CreateDepartmentRequest("CS", "Computer Science");

        when(departmentRepository.existsByCode("CS")).thenReturn(false);
        when(departmentRepository.save(any(Department.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertThatThrownBy(() -> departmentService.createDepartment(request))
                .isInstanceOf(DepartmentCodeAlreadyExistsException.class)
                .hasMessageContaining("CS");
    }

    // Department deliberately has no setters for id/createdAt/updatedAt —
    // that's good encapsulation in production code, but it means tests need
    // another way to build a "realistic, already-saved" Department. Reflection
    // mirrors exactly what Hibernate itself does at runtime to populate these
    // fields, so it's a reasonable, common trade-off here.
    private void setId(Department department, UUID id) {
        setField(department, "id", id);
    }

    private void setTimestamps(Department department, Instant createdAt, Instant updatedAt) {
        setField(department, "createdAt", createdAt);
        setField(department, "updatedAt", updatedAt);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = Department.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
