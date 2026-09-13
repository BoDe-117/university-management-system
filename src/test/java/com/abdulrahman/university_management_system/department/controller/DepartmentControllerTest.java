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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DepartmentController.class)
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DepartmentService departmentService;

    // ── POST /api/v1/departments ────────────────────────────────────────

    @Test
    void createDepartment_returns201WithLocationHeader() throws Exception {
        UUID id = UUID.randomUUID();
        DepartmentResponse response = new DepartmentResponse(
                id, "CS", "Computer Science", Instant.now(), Instant.now());

        when(departmentService.createDepartment(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "CS", "name": "Computer Science"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/departments/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.code").value("CS"))
                .andExpect(jsonPath("$.name").value("Computer Science"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void createDepartment_returns409_whenCodeAlreadyExists() throws Exception {
        when(departmentService.createDepartment(any()))
                .thenThrow(new DepartmentCodeAlreadyExistsException("CS"));

        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "CS", "name": "Computer Science"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void createDepartment_returns400_whenCodeIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "", "name": "Computer Science"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.code").exists());
    }

    @Test
    void createDepartment_returns400_whenNameIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "CS", "name": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void createDepartment_returns400_whenFieldsAreMissing() throws Exception {
        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.code").exists())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void createDepartment_returns400_whenBodyIsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Malformed or missing request body"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void createDepartment_returns400_whenBodyIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Malformed or missing request body"));
    }

    // ── GET /api/v1/departments/{id} ────────────────────────────────────

    @Test
    void getDepartmentById_returns200_whenDepartmentExists() throws Exception {
        UUID id = UUID.randomUUID();
        DepartmentResponse response = new DepartmentResponse(
                id, "CS", "Computer Science", Instant.now(), Instant.now());

        when(departmentService.getDepartmentById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/departments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.code").value("CS"));
    }

    @Test
    void getDepartmentById_returns404_whenDepartmentDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        when(departmentService.getDepartmentById(id))
                .thenThrow(new DepartmentNotFoundException(id));

        mockMvc.perform(get("/api/v1/departments/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getDepartmentById_returns400_whenIdIsNotAValidUuid() throws Exception {
        mockMvc.perform(get("/api/v1/departments/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ── GET /api/v1/departments ─────────────────────────────────────────

    @Test
    void getAllDepartments_returns200_withList() throws Exception {
        DepartmentResponse cs = new DepartmentResponse(
                UUID.randomUUID(), "CS", "Computer Science", Instant.now(), Instant.now());
        DepartmentResponse se = new DepartmentResponse(
                UUID.randomUUID(), "SE", "Software Engineering", Instant.now(), Instant.now());

        when(departmentService.getAllDepartments()).thenReturn(List.of(cs, se));

        mockMvc.perform(get("/api/v1/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("CS"))
                .andExpect(jsonPath("$[1].code").value("SE"));
    }

    @Test
    void getAllDepartments_returns200_withEmptyList() throws Exception {
        when(departmentService.getAllDepartments()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
