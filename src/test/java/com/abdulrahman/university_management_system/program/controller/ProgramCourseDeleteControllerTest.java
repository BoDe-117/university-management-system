package com.abdulrahman.university_management_system.program.controller;

import com.abdulrahman.university_management_system.program.exception.ProgramHasAssociatedCoursesException;
import com.abdulrahman.university_management_system.program.service.ProgramService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProgramController.class)
class ProgramCourseDeleteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProgramService programService;

    @Test
    void deleteProgram_returns409_whenProgramHasCourses() throws Exception {
        UUID programId = UUID.randomUUID();

        doThrow(new ProgramHasAssociatedCoursesException("BSC-CS"))
                .when(programService).deleteProgram(programId);

        mockMvc.perform(delete("/api/v1/programs/{id}", programId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value(
                        "Cannot delete program 'BSC-CS' because courses are assigned to it"
                ))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
