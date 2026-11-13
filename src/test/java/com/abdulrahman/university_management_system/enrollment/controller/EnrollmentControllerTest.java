package com.abdulrahman.university_management_system.enrollment.controller;

import com.abdulrahman.university_management_system.enrollment.dto.EnrollmentResponse;
import com.abdulrahman.university_management_system.enrollment.entity.EnrollmentStatus;
import com.abdulrahman.university_management_system.enrollment.exception.EnrollmentAlreadyExistsException;
import com.abdulrahman.university_management_system.enrollment.exception.EnrollmentNotFoundException;
import com.abdulrahman.university_management_system.enrollment.service.EnrollmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnrollmentController.class)
class EnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnrollmentService enrollmentService;

    @Test
    void createEnrollment_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T10:00:00Z");

        EnrollmentResponse response = new EnrollmentResponse(
                id,
                studentId,
                courseId,
                EnrollmentStatus.ENROLLED,
                now,
                now,
                now
        );

        when(enrollmentService.createEnrollment(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(studentId, courseId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.studentId").value(studentId.toString()))
                .andExpect(jsonPath("$.courseId").value(courseId.toString()))
                .andExpect(jsonPath("$.status").value("ENROLLED"));
    }

    @Test
    void createEnrollment_returns409_whenEnrollmentAlreadyExists()
            throws Exception {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        when(enrollmentService.createEnrollment(any()))
                .thenThrow(new EnrollmentAlreadyExistsException(studentId, courseId));

        mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(studentId, courseId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void createEnrollment_returns400_whenStudentIdIsMissing()
            throws Exception {
        String request = """
                {
                    "courseId": "%s"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.studentId").exists());
    }

    @Test
    void getEnrollmentById_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T10:00:00Z");

        EnrollmentResponse response = new EnrollmentResponse(
                id,
                studentId,
                courseId,
                EnrollmentStatus.ENROLLED,
                now,
                now,
                now
        );

        when(enrollmentService.getEnrollmentById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/enrollments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("ENROLLED"));
    }

    @Test
    void getEnrollmentById_returns404_whenNotFound() throws Exception {
        UUID id = UUID.randomUUID();

        when(enrollmentService.getEnrollmentById(id))
                .thenThrow(new EnrollmentNotFoundException(id));

        mockMvc.perform(get("/api/v1/enrollments/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private String validRequest(UUID studentId, UUID courseId) {
        return """
                {
                    "studentId": "%s",
                    "courseId": "%s"
                }
                """.formatted(studentId, courseId);
    }
}
