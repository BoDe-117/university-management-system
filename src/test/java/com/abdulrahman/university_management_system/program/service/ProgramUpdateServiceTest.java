package com.abdulrahman.university_management_system.program.service;

import com.abdulrahman.university_management_system.department.entity.Department;
import com.abdulrahman.university_management_system.department.exception.DepartmentNotFoundException;
import com.abdulrahman.university_management_system.department.repository.DepartmentRepository;
import com.abdulrahman.university_management_system.program.dto.ProgramResponse;
import com.abdulrahman.university_management_system.program.dto.UpdateProgramRequest;
import com.abdulrahman.university_management_system.program.entity.Program;
import com.abdulrahman.university_management_system.program.exception.ProgramCodeAlreadyExistsException;
import com.abdulrahman.university_management_system.program.exception.ProgramNotFoundException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgramUpdateServiceTest {

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private ProgramService programService;

    @Test
    void updateProgram_updatesFieldsAndMovesDepartment() {
        UUID programId = UUID.randomUUID();
        UUID newDepartmentId = UUID.randomUUID();

        Department oldDepartment = mock(Department.class);
        Department newDepartment = mock(Department.class);

        Program program = new Program("OLD", "Old Program", oldDepartment);

        UpdateProgramRequest request = new UpdateProgramRequest(
                "BSC-SE",
                "Software Engineering",
                newDepartmentId
        );

        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));
        when(programRepository.existsByCodeAndIdNot("BSC-SE", programId))
                .thenReturn(false);
        when(departmentRepository.findById(newDepartmentId))
                .thenReturn(Optional.of(newDepartment));
        when(programRepository.saveAndFlush(program))
                .thenReturn(program);

        when(newDepartment.getId()).thenReturn(newDepartmentId);
        when(newDepartment.getCode()).thenReturn("SE");
        when(newDepartment.getName()).thenReturn("Software Engineering");

        ProgramResponse response = programService.updateProgram(programId, request);

        assertThat(response.code()).isEqualTo("BSC-SE");
        assertThat(response.name()).isEqualTo("Software Engineering");
        assertThat(response.departmentId()).isEqualTo(newDepartmentId);
        assertThat(response.departmentCode()).isEqualTo("SE");
        assertThat(program.getDepartment()).isSameAs(newDepartment);

        verify(programRepository).saveAndFlush(program);
    }

    @Test
    void updateProgram_throwsNotFound_whenProgramDoesNotExist() {
        UUID programId = UUID.randomUUID();
        UpdateProgramRequest request = new UpdateProgramRequest(
                "BSC-CS", "Computer Science", UUID.randomUUID()
        );

        when(programRepository.findById(programId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> programService.updateProgram(programId, request))
                .isInstanceOf(ProgramNotFoundException.class);

        verify(programRepository, never()).saveAndFlush(any(Program.class));
    }

    @Test
    void updateProgram_throwsConflict_whenAnotherProgramUsesCode() {
        UUID programId = UUID.randomUUID();
        Department department = mock(Department.class);
        Program program = new Program("OLD", "Old Program", department);

        UpdateProgramRequest request = new UpdateProgramRequest(
                "BSC-CS", "Computer Science", UUID.randomUUID()
        );

        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));
        when(programRepository.existsByCodeAndIdNot("BSC-CS", programId))
                .thenReturn(true);

        assertThatThrownBy(() -> programService.updateProgram(programId, request))
                .isInstanceOf(ProgramCodeAlreadyExistsException.class);

        verify(programRepository, never()).saveAndFlush(any(Program.class));
    }

    @Test
    void updateProgram_throwsNotFound_whenDepartmentDoesNotExist() {
        UUID programId = UUID.randomUUID();
        UUID missingDepartmentId = UUID.randomUUID();

        Department oldDepartment = mock(Department.class);
        Program program = new Program("OLD", "Old Program", oldDepartment);

        UpdateProgramRequest request = new UpdateProgramRequest(
                "NEW", "New Program", missingDepartmentId
        );

        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));
        when(programRepository.existsByCodeAndIdNot("NEW", programId))
                .thenReturn(false);
        when(departmentRepository.findById(missingDepartmentId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> programService.updateProgram(programId, request))
                .isInstanceOf(DepartmentNotFoundException.class);

        verify(programRepository, never()).saveAndFlush(any(Program.class));
    }

    @Test
    void updateProgram_throwsConflict_whenDatabaseRejectsDuplicateCode() {
        UUID programId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();

        Department department = mock(Department.class);
        Program program = new Program("OLD", "Old Program", department);

        UpdateProgramRequest request = new UpdateProgramRequest(
                "NEW", "New Program", departmentId
        );

        ConstraintViolationException constraintException =
                new ConstraintViolationException(
                        "duplicate code",
                        new SQLException(),
                        "uk_programs_code"
                );

        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));
        when(programRepository.existsByCodeAndIdNot("NEW", programId))
                .thenReturn(false);
        when(departmentRepository.findById(departmentId))
                .thenReturn(Optional.of(department));
        when(programRepository.saveAndFlush(program))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicate code",
                        constraintException
                ));

        assertThatThrownBy(() -> programService.updateProgram(programId, request))
                .isInstanceOf(ProgramCodeAlreadyExistsException.class);
    }

    @Test
    void updateProgram_rethrowsUnrelatedDatabaseError() {
        UUID programId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();

        Department department = mock(Department.class);
        Program program = new Program("OLD", "Old Program", department);

        UpdateProgramRequest request = new UpdateProgramRequest(
                "NEW", "New Program", departmentId
        );

        DataIntegrityViolationException original =
                new DataIntegrityViolationException("other database error");

        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));
        when(programRepository.existsByCodeAndIdNot("NEW", programId))
                .thenReturn(false);
        when(departmentRepository.findById(departmentId))
                .thenReturn(Optional.of(department));
        when(programRepository.saveAndFlush(program))
                .thenThrow(original);

        assertThatThrownBy(() -> programService.updateProgram(programId, request))
                .isSameAs(original);
    }
}
