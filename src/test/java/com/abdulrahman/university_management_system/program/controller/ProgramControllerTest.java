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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProgramController.class)
class ProgramControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProgramService programService;

    @Test
    void createProgram_returns201WithLocationHeader() throws Exception {
        UUID programId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-03T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-10-03T10:00:00Z");

        ProgramResponse response = new ProgramResponse(
                programId,
                "BSC-SE",
                "BSc in Software Engineering",
                departmentId,
                "SE",
                "Software Engineering",
                createdAt,
                updatedAt
        );

        when(programService.createProgram(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/programs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "BSC-SE",
                                  "name": "BSc in Software Engineering",
                                  "departmentId": "%s"
                                }
                                """.formatted(departmentId)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/programs/" + programId
                ))
                .andExpect(jsonPath("$.id").value(programId.toString()))
                .andExpect(jsonPath("$.code").value("BSC-SE"))
                .andExpect(jsonPath("$.name")
                        .value("BSc in Software Engineering"))
                .andExpect(jsonPath("$.departmentId")
                        .value(departmentId.toString()))
                .andExpect(jsonPath("$.departmentCode").value("SE"))
                .andExpect(jsonPath("$.departmentName")
                        .value("Software Engineering"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void createProgram_returns409_whenCodeAlreadyExists() throws Exception {
        UUID departmentId = UUID.randomUUID();

        when(programService.createProgram(any()))
                .thenThrow(new ProgramCodeAlreadyExistsException("BSC-SE"));

        mockMvc.perform(post("/api/v1/programs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "BSC-SE",
                                  "name": "BSc in Software Engineering",
                                  "departmentId": "%s"
                                }
                                """.formatted(departmentId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("A program with code 'BSC-SE' already exists"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void createProgram_returns404_whenDepartmentDoesNotExist() throws Exception {
        UUID departmentId = UUID.randomUUID();

        when(programService.createProgram(any()))
                .thenThrow(new DepartmentNotFoundException(departmentId));

        mockMvc.perform(post("/api/v1/programs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "BSC-SE",
                                  "name": "BSc in Software Engineering",
                                  "departmentId": "%s"
                                }
                                """.formatted(departmentId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void createProgram_returns400_whenCodeIsBlank() throws Exception {
        UUID departmentId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/programs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "",
                                  "name": "BSc in Software Engineering",
                                  "departmentId": "%s"
                                }
                                """.formatted(departmentId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.code").exists());
    }

    @Test
    void createProgram_returns400_whenNameIsBlank() throws Exception {
        UUID departmentId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/programs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "BSC-SE",
                                  "name": "",
                                  "departmentId": "%s"
                                }
                                """.formatted(departmentId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void createProgram_returns400_whenDepartmentIdIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/programs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "BSC-SE",
                                  "name": "BSc in Software Engineering"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.departmentId").exists());
    }

    @Test
    void createProgram_returns400_whenAllFieldsAreMissing() throws Exception {
        mockMvc.perform(post("/api/v1/programs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.code").exists())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.departmentId").exists());
    }

    @Test
    void createProgram_returns400_whenBodyIsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/programs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error")
                        .value("Malformed or missing request body"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void createProgram_returns400_whenBodyIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/programs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error")
                        .value("Malformed or missing request body"));
    }

    @Test
    void getProgramById_returns200_whenProgramExists() throws Exception {
        UUID programId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();

        ProgramResponse response = programResponse(
                programId,
                departmentId,
                "BSC-SE",
                "BSc in Software Engineering"
        );

        when(programService.getProgramById(programId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/programs/{id}", programId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(programId.toString()))
                .andExpect(jsonPath("$.code").value("BSC-SE"))
                .andExpect(jsonPath("$.name")
                        .value("BSc in Software Engineering"))
                .andExpect(jsonPath("$.departmentId")
                        .value(departmentId.toString()))
                .andExpect(jsonPath("$.departmentCode").value("SE"));
    }

    @Test
    void getProgramById_returns404_whenProgramDoesNotExist() throws Exception {
        UUID programId = UUID.randomUUID();

        when(programService.getProgramById(programId))
                .thenThrow(new ProgramNotFoundException(programId));

        mockMvc.perform(get("/api/v1/programs/{id}", programId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getProgramById_returns400_whenIdIsNotAValidUuid() throws Exception {
        mockMvc.perform(get("/api/v1/programs/not-a-valid-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getAllPrograms_returns200WithList() throws Exception {
        ProgramResponse firstProgram = programResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BSC-SE",
                "BSc in Software Engineering"
        );

        ProgramResponse secondProgram = programResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BSC-CS",
                "BSc in Computer Science"
        );

        when(programService.getAllPrograms())
                .thenReturn(List.of(firstProgram, secondProgram));

        mockMvc.perform(get("/api/v1/programs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("BSC-SE"))
                .andExpect(jsonPath("$[1].code").value("BSC-CS"));

        verify(programService).getAllPrograms();
    }

    @Test
    void getAllPrograms_returns200WithEmptyList() throws Exception {
        when(programService.getAllPrograms()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/programs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(programService).getAllPrograms();
    }

    @Test
    void getProgramsByDepartment_returns200WithFilteredList()
            throws Exception {
        UUID departmentId = UUID.randomUUID();

        ProgramResponse response = programResponse(
                UUID.randomUUID(),
                departmentId,
                "BSC-SE",
                "BSc in Software Engineering"
        );

        when(programService.getProgramsByDepartmentId(departmentId))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/programs")
                        .queryParam("departmentId", departmentId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("BSC-SE"))
                .andExpect(jsonPath("$[0].departmentId")
                        .value(departmentId.toString()));

        verify(programService).getProgramsByDepartmentId(departmentId);
    }

    @Test
    void getProgramsByDepartment_returns200WithEmptyList()
            throws Exception {
        UUID departmentId = UUID.randomUUID();

        when(programService.getProgramsByDepartmentId(departmentId))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/programs")
                        .queryParam("departmentId", departmentId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(programService).getProgramsByDepartmentId(departmentId);
    }

    @Test
    void getProgramsByDepartment_returns404_whenDepartmentDoesNotExist()
            throws Exception {
        UUID departmentId = UUID.randomUUID();

        when(programService.getProgramsByDepartmentId(departmentId))
                .thenThrow(new DepartmentNotFoundException(departmentId));

        mockMvc.perform(get("/api/v1/programs")
                        .queryParam("departmentId", departmentId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getProgramsByDepartment_returns400_whenDepartmentIdIsInvalid()
            throws Exception {
        mockMvc.perform(get("/api/v1/programs")
                        .queryParam("departmentId", "not-a-valid-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    private ProgramResponse programResponse(
            UUID programId,
            UUID departmentId,
            String code,
            String name
    ) {
        Instant timestamp = Instant.parse("2026-10-03T10:00:00Z");

        return new ProgramResponse(
                programId,
                code,
                name,
                departmentId,
                "SE",
                "Software Engineering",
                timestamp,
                timestamp
        );
    }
}
