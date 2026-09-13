package com.abdulrahman.university_management_system.department.service;

import com.abdulrahman.university_management_system.department.dto.CreateDepartmentRequest;
import com.abdulrahman.university_management_system.department.dto.DepartmentResponse;
import com.abdulrahman.university_management_system.department.entity.Department;
import com.abdulrahman.university_management_system.department.exception.DepartmentCodeAlreadyExistsException;
import com.abdulrahman.university_management_system.department.exception.DepartmentNotFoundException;
import com.abdulrahman.university_management_system.department.repository.DepartmentRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private DepartmentService departmentService;

    // ── create: happy path ──────────────────────────────────────────────

    @Test
    void createDepartment_savesAndReturnsResponse_whenCodeIsUnique() {
        CreateDepartmentRequest request = new CreateDepartmentRequest("CS", "Computer Science");

        UUID expectedId = UUID.randomUUID();
        Instant expectedCreatedAt = Instant.now();
        Instant expectedUpdatedAt = Instant.now();

        Department saved = new Department("CS", "Computer Science");
        setId(saved, expectedId);
        setTimestamps(saved, expectedCreatedAt, expectedUpdatedAt);

        when(departmentRepository.existsByCode("CS")).thenReturn(false);
        when(departmentRepository.saveAndFlush(any(Department.class))).thenReturn(saved);

        DepartmentResponse response = departmentService.createDepartment(request);

        assertThat(response.code()).isEqualTo("CS");
        assertThat(response.name()).isEqualTo("Computer Science");
        assertThat(response.id()).isEqualTo(expectedId);
        assertThat(response.createdAt()).isEqualTo(expectedCreatedAt);
        assertThat(response.updatedAt()).isEqualTo(expectedUpdatedAt);
    }

    @Test
    void createDepartment_passesCorrectCodeAndNameToRepository() {
        CreateDepartmentRequest request = new CreateDepartmentRequest("SE", "Software Engineering");

        Department saved = new Department("SE", "Software Engineering");
        setId(saved, UUID.randomUUID());
        setTimestamps(saved, Instant.now(), Instant.now());

        when(departmentRepository.existsByCode("SE")).thenReturn(false);
        when(departmentRepository.saveAndFlush(any(Department.class))).thenReturn(saved);

        departmentService.createDepartment(request);

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).saveAndFlush(captor.capture());

        Department captured = captor.getValue();
        assertThat(captured.getCode()).isEqualTo("SE");
        assertThat(captured.getName()).isEqualTo("Software Engineering");
    }

    // ── create: pre-check duplicate ─────────────────────────────────────

    @Test
    void createDepartment_throwsConflict_whenCodeAlreadyExists() {
        CreateDepartmentRequest request = new CreateDepartmentRequest("CS", "Computer Science");
        when(departmentRepository.existsByCode("CS")).thenReturn(true);

        assertThatThrownBy(() -> departmentService.createDepartment(request))
                .isInstanceOf(DepartmentCodeAlreadyExistsException.class)
                .hasMessageContaining("CS");

        verify(departmentRepository, never()).saveAndFlush(any());
    }

    // ── create: race condition — known constraint ───────────────────────

    @Test
    void createDepartment_throwsConflict_whenDatabaseRejectsDuplicateCode() {
        CreateDepartmentRequest request = new CreateDepartmentRequest("CS", "Computer Science");

        when(departmentRepository.existsByCode("CS")).thenReturn(false);

        ConstraintViolationException hibernateEx = new ConstraintViolationException(
                "duplicate key",
                new SQLException(),
                "departments_code_key"
        );
        when(departmentRepository.saveAndFlush(any(Department.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key", hibernateEx));

        assertThatThrownBy(() -> departmentService.createDepartment(request))
                .isInstanceOf(DepartmentCodeAlreadyExistsException.class)
                .hasMessageContaining("CS");

        verify(departmentRepository, times(1)).existsByCode("CS");
    }

    // ── create: race condition — unknown constraint ─────────────────────

    @Test
    void createDepartment_rethrowsException_whenConstraintIsNotDuplicateCode() {
        CreateDepartmentRequest request = new CreateDepartmentRequest("CS", "Computer Science");

        when(departmentRepository.existsByCode("CS")).thenReturn(false);

        ConstraintViolationException hibernateEx = new ConstraintViolationException(
                "some other constraint",
                new SQLException(),
                "some_other_constraint_name"
        );
        DataIntegrityViolationException original = new DataIntegrityViolationException(
                "some other constraint", hibernateEx);

        when(departmentRepository.saveAndFlush(any(Department.class))).thenThrow(original);

        assertThatThrownBy(() -> departmentService.createDepartment(request))
                .isSameAs(original);
    }

    // ── create: race condition — no ConstraintViolationException cause ──

    @Test
    void createDepartment_rethrowsException_whenCauseIsNotConstraintViolation() {
        CreateDepartmentRequest request = new CreateDepartmentRequest("CS", "Computer Science");

        when(departmentRepository.existsByCode("CS")).thenReturn(false);

        DataIntegrityViolationException original = new DataIntegrityViolationException(
                "unexpected error", new RuntimeException("not a constraint violation"));

        when(departmentRepository.saveAndFlush(any(Department.class))).thenThrow(original);

        assertThatThrownBy(() -> departmentService.createDepartment(request))
                .isSameAs(original);
    }

    // ── create: race condition — null constraint name ───────────────────

    @Test
    void createDepartment_rethrowsException_whenConstraintNameIsNull() {
        CreateDepartmentRequest request = new CreateDepartmentRequest("CS", "Computer Science");

        when(departmentRepository.existsByCode("CS")).thenReturn(false);

        ConstraintViolationException hibernateEx = new ConstraintViolationException(
                "constraint violation",
                new SQLException(),
                null
        );
        DataIntegrityViolationException original = new DataIntegrityViolationException(
                "constraint violation", hibernateEx);

        when(departmentRepository.saveAndFlush(any(Department.class))).thenThrow(original);

        assertThatThrownBy(() -> departmentService.createDepartment(request))
                .isSameAs(original);
    }

    // ── get by ID ───────────────────────────────────────────────────────

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

    // ── list all ────────────────────────────────────────────────────────

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

    // ── helpers ─────────────────────────────────────────────────────────

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

