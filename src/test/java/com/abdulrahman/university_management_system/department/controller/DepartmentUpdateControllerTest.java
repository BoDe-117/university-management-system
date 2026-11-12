package com.abdulrahman.university_management_system.department.controller;

import com.abdulrahman.university_management_system.department.dto.DepartmentResponse;
import com.abdulrahman.university_management_system.department.exception.DepartmentCodeAlreadyExistsException;
import com.abdulrahman.university_management_system.department.exception.DepartmentNotFoundException;
import com.abdulrahman.university_management_system.department.service.DepartmentService;
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

@WebMvcTest(DepartmentController.class)
class DepartmentUpdateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DepartmentService departmentService;

    @Test
    void updateDepartment_returns200_whenRequestIsValid() throws Exception {
        UUID id = UUID.randomUUID();
        Instant timestamp = Instant.parse("2026-01-01T10:00:00Z");

        DepartmentResponse response = new DepartmentResponse(
                id,
                "SE",
                "Software Engineering",
                timestamp,
                timestamp
        );

        when(departmentService.updateDepartment(eq(id), any()))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/departments/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "SE",
                                  "name": "Software Engineering"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.code").value("SE"))
                .andExpect(jsonPath("$.name").value("Software Engineering"));
    }

    @Test
    void updateDepartment_returns404_whenDepartmentDoesNotExist()
            throws Exception {
        UUID id = UUID.randomUUID();

        when(departmentService.updateDepartment(eq(id), any()))
                .thenThrow(new DepartmentNotFoundException(id));

        mockMvc.perform(put("/api/v1/departments/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "SE",
                                  "name": "Software Engineering"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void updateDepartment_returns409_whenCodeAlreadyExists() throws Exception {
        UUID id = UUID.randomUUID();

        when(departmentService.updateDepartment(eq(id), any()))
                .thenThrow(new DepartmentCodeAlreadyExistsException("SE"));

        mockMvc.perform(put("/api/v1/departments/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "SE",
                                  "name": "Software Engineering"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void updateDepartment_returns400_whenCodeIsBlank() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/departments/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "",
                                  "name": "Software Engineering"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.code").exists());
    }

    @Test
    void updateDepartment_returns400_whenNameIsBlank() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/departments/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "SE",
                                  "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void updateDepartment_returns400_whenIdIsInvalid() throws Exception {
        mockMvc.perform(put("/api/v1/departments/not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "SE",
                                  "name": "Software Engineering"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateDepartment_returns400_whenBodyIsMissing() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/departments/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error")
                        .value("Malformed or missing request body"));
    }
}
