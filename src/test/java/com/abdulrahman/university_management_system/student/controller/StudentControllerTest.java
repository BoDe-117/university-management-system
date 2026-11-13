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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentController.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @Test
    void createStudent_returns201_whenRequestIsValid() throws Exception {
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

        when(studentService.createStudent(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(programId)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location", "/api/v1/students/" + id
                ))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.studentId").value("STU-001"))
                .andExpect(jsonPath("$.email").value("sara@example.com"))
                .andExpect(jsonPath("$.programId").value(programId.toString()));
    }

    @Test
    void createStudent_returns409_whenStudentIdExists() throws Exception {
        when(studentService.createStudent(any()))
                .thenThrow(new StudentIdAlreadyExistsException("STU-001"));

        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void createStudent_returns409_whenEmailExists() throws Exception {
        when(studentService.createStudent(any()))
                .thenThrow(new StudentEmailAlreadyExistsException("sara@example.com"));

        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void createStudent_returns404_whenProgramDoesNotExist() throws Exception {
        UUID programId = UUID.randomUUID();

        when(studentService.createStudent(any()))
                .thenThrow(new ProgramNotFoundException(programId));

        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(programId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void createStudent_returns400_whenFieldsAreMissing() throws Exception {
        mockMvc.perform(post("/api/v1/students")
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
    void createStudent_returns400_whenEmailIsInvalid() throws Exception {
        UUID programId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "studentId": "STU-001",
                                  "firstName": "Sara",
                                  "lastName": "Ahmed",
                                  "email": "not-an-email@",
                                  "programId": "%s"
                                }
                                """.formatted(programId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void getStudentById_returns404_whenStudentDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();

        when(studentService.getStudentById(id))
                .thenThrow(new StudentNotFoundException(id));

        mockMvc.perform(get("/api/v1/students/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getStudents_returnsStudentsForProgram() throws Exception {
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

        when(studentService.getStudentsByProgramId(programId))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/students")
                        .queryParam("programId", programId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].studentId").value("STU-001"));
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
