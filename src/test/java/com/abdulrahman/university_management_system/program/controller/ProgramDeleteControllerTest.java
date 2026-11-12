package com.abdulrahman.university_management_system.program.controller;

import com.abdulrahman.university_management_system.program.exception.ProgramNotFoundException;
import com.abdulrahman.university_management_system.program.service.ProgramService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProgramController.class)
class ProgramDeleteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProgramService programService;

    @Test
    void deleteProgram_returns204_whenDeletionSucceeds() throws Exception {
        UUID programId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/programs/{id}", programId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(programService).deleteProgram(programId);
    }

    @Test
    void deleteProgram_returns404_whenProgramDoesNotExist() throws Exception {
        UUID programId = UUID.randomUUID();

        doThrow(new ProgramNotFoundException(programId))
                .when(programService).deleteProgram(programId);

        mockMvc.perform(delete("/api/v1/programs/{id}", programId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void deleteProgram_returns400_whenIdIsInvalid() throws Exception {
        mockMvc.perform(delete("/api/v1/programs/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
