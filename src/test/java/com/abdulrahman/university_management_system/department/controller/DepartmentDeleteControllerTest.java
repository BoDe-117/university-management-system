package com.abdulrahman.university_management_system.department.controller;

import com.abdulrahman.university_management_system.department.exception.DepartmentHasAssociatedProgramsException;
import com.abdulrahman.university_management_system.department.exception.DepartmentNotFoundException;
import com.abdulrahman.university_management_system.department.service.DepartmentService;
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

@WebMvcTest(DepartmentController.class)
class DepartmentDeleteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DepartmentService departmentService;

    @Test
    void deleteDepartment_returns204_whenDeletionSucceeds() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/departments/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(departmentService).deleteDepartment(id);
    }

    @Test
    void deleteDepartment_returns404_whenDepartmentDoesNotExist()
            throws Exception {
        UUID id = UUID.randomUUID();

        doThrow(new DepartmentNotFoundException(id))
                .when(departmentService).deleteDepartment(id);

        mockMvc.perform(delete("/api/v1/departments/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void deleteDepartment_returns409_whenDepartmentHasPrograms()
            throws Exception {
        UUID id = UUID.randomUUID();

        doThrow(new DepartmentHasAssociatedProgramsException("CS"))
                .when(departmentService).deleteDepartment(id);

        mockMvc.perform(delete("/api/v1/departments/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error")
                        .value("Cannot delete department 'CS' because programs are assigned to it"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void deleteDepartment_returns400_whenIdIsInvalid() throws Exception {
        mockMvc.perform(delete("/api/v1/departments/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
