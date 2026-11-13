package com.abdulrahman.university_management_system.student.controller;

import com.abdulrahman.university_management_system.program.exception.ProgramNotFoundException;
import com.abdulrahman.university_management_system.student.dto.StudentResponse;
import com.abdulrahman.university_management_system.student.exception.StudentEmailAlreadyExistsException;
import com.abdulrahman.university_management_system.student.exception.StudentIdAlreadyExistsException;
import com.abdulrahman.university_management_system.student.exception.StudentNotFoundException;
import com.abdulrahman.university_management_system.student.service.StudentService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentController.class)
class StudentUpdateDeleteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @Test
    void updateStudent_returns200_whenRequestIsValid() throws Exception {
        UUID id = UUID.randomUUID();
        UUID programId = UUID.randomUUID();
        Instant timestamp = Instant.parse("2026-01-01T10:00:00Z");

        StudentResponse response = new StudentResponse(
                id,
                "STU-001",
                "Sara",
                "Ahmed",
                "sara@example.com",
                programId,
                "BSC-CS",
                "Computer Science",
                timestamp,
                timestamp
        );

        when(studentService.updateStudent(eq(id), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/students/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(programId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.studentId").value("STU-001"))
                .andExpect(jsonPath("$.programId").value(programId.toString()));
    }

    @Test
    void updateStudent_returns404_whenStudentDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();

        when(studentService.updateStudent(eq(id), any()))
                .thenThrow(new StudentNotFoundException(id));

        mockMvc.perform(put("/api/v1/students/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateStudent_returns404_whenProgramDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        UUID programId = UUID.randomUUID();

        when(studentService.updateStudent(eq(id), any()))
                .thenThrow(new ProgramNotFoundException(programId));

        mockMvc.perform(put("/api/v1/students/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(programId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateStudent_returns409_whenStudentIdExists() throws Exception {
        UUID id = UUID.randomUUID();

        when(studentService.updateStudent(eq(id), any()))
                .thenThrow(new StudentIdAlreadyExistsException("STU-001"));

        mockMvc.perform(put("/api/v1/students/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void updateStudent_returns409_whenEmailExists() throws Exception {
        UUID id = UUID.randomUUID();

        when(studentService.updateStudent(eq(id), any()))
                .thenThrow(new StudentEmailAlreadyExistsException("sara@example.com"));

        mockMvc.perform(put("/api/v1/students/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void updateStudent_returns400_whenFieldsAreMissing() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(put("/api/v1/students/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.studentId").exists())
                .andExpect(jsonPath("$.fieldErrors.firstName").exists())
                .andExpect(jsonPath("$.fieldErrors.lastName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.programId").exists());
    }

    @Test
    void deleteStudent_returns204_whenDeletionSucceeds() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/students/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(studentService).deleteStudent(id);
    }

    @Test
    void deleteStudent_returns404_whenStudentDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();

        doThrow(new StudentNotFoundException(id))
                .when(studentService).deleteStudent(id);

        mockMvc.perform(delete("/api/v1/students/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deleteStudent_returns400_whenIdIsInvalid() throws Exception {
        mockMvc.perform(delete("/api/v1/students/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private String validRequest(UUID programId) {
        return """
                {
                  "studentId": "STU-001",
                  "firstName": "Sara",
                  "lastName": "Ahmed",
                  "email": "sara@example.com",
                  "programId": "%s"
                }
                """.formatted(programId);
    }
}
