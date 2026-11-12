package com.abdulrahman.university_management_system.program.controller;

import com.abdulrahman.university_management_system.department.exception.DepartmentNotFoundException;
import com.abdulrahman.university_management_system.program.dto.ProgramResponse;
import com.abdulrahman.university_management_system.program.exception.ProgramCodeAlreadyExistsException;
import com.abdulrahman.university_management_system.program.exception.ProgramNotFoundException;
import com.abdulrahman.university_management_system.program.service.ProgramService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProgramController.class)
class ProgramUpdateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProgramService programService;

    @Test
    void updateProgram_returns200_whenRequestIsValid() throws Exception {
        UUID programId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        Instant timestamp = Instant.parse("2026-01-01T10:00:00Z");

        ProgramResponse response = new ProgramResponse(
                programId,
                "BSC-SE",
                "Software Engineering",
                departmentId,
                "SE",
                "Software Engineering",
                timestamp,
                timestamp
        );

        when(programService.updateProgram(eq(programId), any()))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/programs/{id}", programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "BSC-SE",
                                  "name": "Software Engineering",
                                  "departmentId": "%s"
                                }
                                """.formatted(departmentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(programId.toString()))
                .andExpect(jsonPath("$.code").value("BSC-SE"))
                .andExpect(jsonPath("$.departmentId")
                        .value(departmentId.toString()));
    }

    @Test
    void updateProgram_returns404_whenProgramDoesNotExist() throws Exception {
        UUID programId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();

        when(programService.updateProgram(eq(programId), any()))
                .thenThrow(new ProgramNotFoundException(programId));

        mockMvc.perform(put("/api/v1/programs/{id}", programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "BSC-SE",
                                  "name": "Software Engineering",
                                  "departmentId": "%s"
                                }
                                """.formatted(departmentId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateProgram_returns404_whenDepartmentDoesNotExist() throws Exception {
        UUID programId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();

        when(programService.updateProgram(eq(programId), any()))
                .thenThrow(new DepartmentNotFoundException(departmentId));

        mockMvc.perform(put("/api/v1/programs/{id}", programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "BSC-SE",
                                  "name": "Software Engineering",
                                  "departmentId": "%s"
                                }
                                """.formatted(departmentId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateProgram_returns409_whenCodeAlreadyExists() throws Exception {
        UUID programId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();

        when(programService.updateProgram(eq(programId), any()))
                .thenThrow(new ProgramCodeAlreadyExistsException("BSC-SE"));

        mockMvc.perform(put("/api/v1/programs/{id}", programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "BSC-SE",
                                  "name": "Software Engineering",
                                  "departmentId": "%s"
                                }
                                """.formatted(departmentId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void updateProgram_returns400_whenFieldsAreMissing() throws Exception {
        UUID programId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/programs/{id}", programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.code").exists())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.departmentId").exists());
    }

    @Test
    void updateProgram_returns400_whenFieldsAreTooLong() throws Exception {
        UUID programId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/programs/{id}", programId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "%s",
                                  "name": "%s",
                                  "departmentId": "%s"
                                }
                                """.formatted(
                                "A".repeat(21),
                                "B".repeat(151),
                                departmentId
                        )))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.code").exists())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void updateProgram_returns400_whenIdIsInvalid() throws Exception {
        mockMvc.perform(put("/api/v1/programs/not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "BSC-SE",
                                  "name": "Software Engineering",
                                  "departmentId": "%s"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateProgram_returns400_whenBodyIsMissing() throws Exception {
        mockMvc.perform(put("/api/v1/programs/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error")
                        .value("Malformed or missing request body"));
    }
}
