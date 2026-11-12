package com.abdulrahman.university_management_system.program.service;

import com.abdulrahman.university_management_system.department.entity.Department;
import com.abdulrahman.university_management_system.department.exception.DepartmentNotFoundException;
import com.abdulrahman.university_management_system.department.repository.DepartmentRepository;
import com.abdulrahman.university_management_system.program.dto.CreateProgramRequest;
import com.abdulrahman.university_management_system.program.dto.ProgramResponse;
import com.abdulrahman.university_management_system.program.entity.Program;
import com.abdulrahman.university_management_system.program.exception.ProgramCodeAlreadyExistsException;
import com.abdulrahman.university_management_system.program.exception.ProgramNotFoundException;
import com.abdulrahman.university_management_system.program.repository.ProgramRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgramServiceTest {

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private ProgramService programService;

    @Test
    void createProgram_savesAndReturnsProgram_whenRequestIsValid() {
        UUID departmentId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-09-13T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-09-13T10:00:00Z");

        CreateProgramRequest request = new CreateProgramRequest(
                "BSC-SE",
                "BSc in Software Engineering",
                departmentId
        );

        Department department = mockDepartment(
                departmentId,
                "SE",
                "Software Engineering"
        );

        Program savedProgram = mockProgram(
                programId,
                "BSC-SE",
                "BSc in Software Engineering",
                department,
                createdAt,
                updatedAt
        );

        when(programRepository.existsByCode("BSC-SE")).thenReturn(false);
        when(departmentRepository.findById(departmentId))
                .thenReturn(Optional.of(department));
        when(programRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(Program.class)))
                .thenReturn(savedProgram);

        ProgramResponse response = programService.createProgram(request);

        assertEquals(programId, response.id());
        assertEquals("BSC-SE", response.code());
        assertEquals("BSc in Software Engineering", response.name());
        assertEquals(departmentId, response.departmentId());
        assertEquals("SE", response.departmentCode());
        assertEquals("Software Engineering", response.departmentName());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());

        ArgumentCaptor<Program> captor = ArgumentCaptor.forClass(Program.class);
        verify(programRepository).saveAndFlush(captor.capture());

        Program programToSave = captor.getValue();

        assertEquals("BSC-SE", programToSave.getCode());
        assertEquals("BSc in Software Engineering", programToSave.getName());
        assertSame(department, programToSave.getDepartment());
    }

    @Test
    void createProgram_throwsConflict_whenCodeAlreadyExists() {
        UUID departmentId = UUID.randomUUID();

        CreateProgramRequest request = new CreateProgramRequest(
                "BSC-SE",
                "BSc in Software Engineering",
                departmentId
        );

        when(programRepository.existsByCode("BSC-SE")).thenReturn(true);

        ProgramCodeAlreadyExistsException exception = assertThrows(
                ProgramCodeAlreadyExistsException.class,
                () -> programService.createProgram(request)
        );

        assertEquals(
                "A program with code 'BSC-SE' already exists",
                exception.getMessage()
        );

        verify(departmentRepository, never()).findById(departmentId);
        verify(programRepository, never())
                .saveAndFlush(org.mockito.ArgumentMatchers.any(Program.class));
    }

    @Test
    void createProgram_throwsDepartmentNotFound_whenDepartmentDoesNotExist() {
        UUID departmentId = UUID.randomUUID();

        CreateProgramRequest request = new CreateProgramRequest(
                "BSC-SE",
                "BSc in Software Engineering",
                departmentId
        );

        when(programRepository.existsByCode("BSC-SE")).thenReturn(false);
        when(departmentRepository.findById(departmentId))
                .thenReturn(Optional.empty());

        DepartmentNotFoundException exception = assertThrows(
                DepartmentNotFoundException.class,
                () -> programService.createProgram(request)
        );

        assertEquals(
                "Department not found with id: " + departmentId,
                exception.getMessage()
        );

        verify(programRepository, never())
                .saveAndFlush(org.mockito.ArgumentMatchers.any(Program.class));
    }

    @Test
    void createProgram_throwsConflict_whenDatabaseRejectsDuplicateCode() {
        UUID departmentId = UUID.randomUUID();

        CreateProgramRequest request = new CreateProgramRequest(
                "BSC-SE",
                "BSc in Software Engineering",
                departmentId
        );

        Department department = mock(Department.class);

        ConstraintViolationException constraintViolation =
                new ConstraintViolationException(
                        "Duplicate program code",
                        new SQLException(),
                        "uk_programs_code"
                );

        DataIntegrityViolationException dataIntegrityViolation =
                new DataIntegrityViolationException(
                        "Database constraint violation",
                        constraintViolation
                );

        when(programRepository.existsByCode("BSC-SE")).thenReturn(false);
        when(departmentRepository.findById(departmentId))
                .thenReturn(Optional.of(department));
        when(programRepository.saveAndFlush(
                org.mockito.ArgumentMatchers.any(Program.class)
        )).thenThrow(dataIntegrityViolation);

        ProgramCodeAlreadyExistsException exception = assertThrows(
                ProgramCodeAlreadyExistsException.class,
                () -> programService.createProgram(request)
        );

        assertEquals(
                "A program with code 'BSC-SE' already exists",
                exception.getMessage()
        );
    }

    @Test
    void createProgram_rethrowsException_whenConstraintIsDifferent() {
        UUID departmentId = UUID.randomUUID();

        CreateProgramRequest request = new CreateProgramRequest(
                "BSC-SE",
                "BSc in Software Engineering",
                departmentId
        );

        Department department = mock(Department.class);

        ConstraintViolationException constraintViolation =
                new ConstraintViolationException(
                        "Different database constraint",
                        new SQLException(),
                        "fk_programs_department"
                );

        DataIntegrityViolationException dataIntegrityViolation =
                new DataIntegrityViolationException(
                        "Database constraint violation",
                        constraintViolation
                );

        when(programRepository.existsByCode("BSC-SE")).thenReturn(false);
        when(departmentRepository.findById(departmentId))
                .thenReturn(Optional.of(department));
        when(programRepository.saveAndFlush(
                org.mockito.ArgumentMatchers.any(Program.class)
        )).thenThrow(dataIntegrityViolation);

        DataIntegrityViolationException thrown = assertThrows(
                DataIntegrityViolationException.class,
                () -> programService.createProgram(request)
        );

        assertSame(dataIntegrityViolation, thrown);
    }

    @Test
    void createProgram_rethrowsException_whenCauseIsNotConstraintViolation() {
        UUID departmentId = UUID.randomUUID();

        CreateProgramRequest request = new CreateProgramRequest(
                "BSC-SE",
                "BSc in Software Engineering",
                departmentId
        );

        Department department = mock(Department.class);

        DataIntegrityViolationException dataIntegrityViolation =
                new DataIntegrityViolationException(
                        "Unknown database error",
                        new RuntimeException("Unknown cause")
                );

        when(programRepository.existsByCode("BSC-SE")).thenReturn(false);
        when(departmentRepository.findById(departmentId))
                .thenReturn(Optional.of(department));
        when(programRepository.saveAndFlush(
                org.mockito.ArgumentMatchers.any(Program.class)
        )).thenThrow(dataIntegrityViolation);

        DataIntegrityViolationException thrown = assertThrows(
                DataIntegrityViolationException.class,
                () -> programService.createProgram(request)
        );

        assertSame(dataIntegrityViolation, thrown);
    }

    @Test
    void getProgramById_returnsProgram_whenProgramExists() {
        UUID departmentId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-09-13T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-09-13T11:00:00Z");

        Department department = mockDepartment(
                departmentId,
                "SE",
                "Software Engineering"
        );

        Program program = mockProgram(
                programId,
                "BSC-SE",
                "BSc in Software Engineering",
                department,
                createdAt,
                updatedAt
        );

        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));

        ProgramResponse response = programService.getProgramById(programId);

        assertEquals(programId, response.id());
        assertEquals("BSC-SE", response.code());
        assertEquals("BSc in Software Engineering", response.name());
        assertEquals(departmentId, response.departmentId());
        assertEquals("SE", response.departmentCode());
        assertEquals("Software Engineering", response.departmentName());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
    }

    @Test
    void getProgramById_throwsNotFound_whenProgramDoesNotExist() {
        UUID programId = UUID.randomUUID();

        when(programRepository.findById(programId))
                .thenReturn(Optional.empty());

        ProgramNotFoundException exception = assertThrows(
                ProgramNotFoundException.class,
                () -> programService.getProgramById(programId)
        );

        assertEquals(
                "Program not found with id: " + programId,
                exception.getMessage()
        );
    }

    @Test
    void getAllPrograms_returnsMappedPrograms() {
        UUID firstDepartmentId = UUID.randomUUID();
        UUID secondDepartmentId = UUID.randomUUID();

        Department firstDepartment = mockDepartment(
                firstDepartmentId,
                "SE",
                "Software Engineering"
        );

        Department secondDepartment = mockDepartment(
                secondDepartmentId,
                "CS",
                "Computer Science"
        );

        Program firstProgram = mockProgram(
                UUID.randomUUID(),
                "BSC-SE",
                "BSc in Software Engineering",
                firstDepartment,
                Instant.parse("2026-09-13T10:00:00Z"),
                Instant.parse("2026-09-13T10:00:00Z")
        );

        Program secondProgram = mockProgram(
                UUID.randomUUID(),
                "BSC-CS",
                "BSc in Computer Science",
                secondDepartment,
                Instant.parse("2026-09-13T11:00:00Z"),
                Instant.parse("2026-09-13T11:00:00Z")
        );

        when(programRepository.findAll())
                .thenReturn(List.of(firstProgram, secondProgram));

        List<ProgramResponse> responses = programService.getAllPrograms();

        assertEquals(2, responses.size());
        assertEquals("BSC-SE", responses.get(0).code());
        assertEquals("SE", responses.get(0).departmentCode());
        assertEquals("BSC-CS", responses.get(1).code());
        assertEquals("CS", responses.get(1).departmentCode());
    }

    @Test
    void getProgramsByDepartmentId_returnsPrograms_whenDepartmentExists() {
        UUID departmentId = UUID.randomUUID();

        Department department = mockDepartment(
                departmentId,
                "SE",
                "Software Engineering"
        );

        Program program = mockProgram(
                UUID.randomUUID(),
                "BSC-SE",
                "BSc in Software Engineering",
                department,
                Instant.parse("2026-09-13T10:00:00Z"),
                Instant.parse("2026-09-13T10:00:00Z")
        );

        when(departmentRepository.existsById(departmentId)).thenReturn(true);
        when(programRepository.findAllByDepartmentId(departmentId))
                .thenReturn(List.of(program));

        List<ProgramResponse> responses =
                programService.getProgramsByDepartmentId(departmentId);

        assertEquals(1, responses.size());
        assertEquals("BSC-SE", responses.getFirst().code());
        assertEquals(departmentId, responses.getFirst().departmentId());

        verify(programRepository).findAllByDepartmentId(departmentId);
    }

    @Test
    void getProgramsByDepartmentId_throwsDepartmentNotFound_whenDepartmentDoesNotExist() {
        UUID departmentId = UUID.randomUUID();

        when(departmentRepository.existsById(departmentId)).thenReturn(false);

        DepartmentNotFoundException exception = assertThrows(
                DepartmentNotFoundException.class,
                () -> programService.getProgramsByDepartmentId(departmentId)
        );

        assertEquals(
                "Department not found with id: " + departmentId,
                exception.getMessage()
        );

        verify(programRepository, never())
                .findAllByDepartmentId(departmentId);
    }

    private Department mockDepartment(
            UUID id,
            String code,
            String name
    ) {
        Department department = mock(Department.class);

        when(department.getId()).thenReturn(id);
        when(department.getCode()).thenReturn(code);
        when(department.getName()).thenReturn(name);

        return department;
    }

    private Program mockProgram(
            UUID id,
            String code,
            String name,
            Department department,
            Instant createdAt,
            Instant updatedAt
    ) {
        Program program = mock(Program.class);

        when(program.getId()).thenReturn(id);
        when(program.getCode()).thenReturn(code);
        when(program.getName()).thenReturn(name);
        when(program.getDepartment()).thenReturn(department);
        when(program.getCreatedAt()).thenReturn(createdAt);
        when(program.getUpdatedAt()).thenReturn(updatedAt);

        return program;
    }
}

