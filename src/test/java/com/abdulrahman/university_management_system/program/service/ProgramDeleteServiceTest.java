package com.abdulrahman.university_management_system.program.service;

import com.abdulrahman.university_management_system.department.entity.Department;
import com.abdulrahman.university_management_system.department.repository.DepartmentRepository;
import com.abdulrahman.university_management_system.program.entity.Program;
import com.abdulrahman.university_management_system.program.exception.ProgramNotFoundException;
import com.abdulrahman.university_management_system.program.repository.ProgramRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgramDeleteServiceTest {

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private ProgramService programService;

    @Test
    void deleteProgram_deletesProgram_whenItExists() {
        UUID programId = UUID.randomUUID();
        Department department = mock(Department.class);
        Program program = new Program("BSC-CS", "Computer Science", department);

        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));

        programService.deleteProgram(programId);

        verify(programRepository).delete(program);
    }

    @Test
    void deleteProgram_throwsNotFound_whenProgramDoesNotExist() {
        UUID programId = UUID.randomUUID();

        when(programRepository.findById(programId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> programService.deleteProgram(programId))
                .isInstanceOf(ProgramNotFoundException.class);

        verify(programRepository, never()).delete(any(Program.class));
    }
}
