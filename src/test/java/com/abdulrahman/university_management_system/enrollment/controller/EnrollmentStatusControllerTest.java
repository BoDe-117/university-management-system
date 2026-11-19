package com.abdulrahman.university_management_system.enrollment.controller;

import com.abdulrahman.university_management_system.enrollment.dto.EnrollmentResponse;
import com.abdulrahman.university_management_system.enrollment.entity.EnrollmentStatus;
import com.abdulrahman.university_management_system.enrollment.exception.EnrollmentNotFoundException;
import com.abdulrahman.university_management_system.enrollment.exception.InvalidEnrollmentStatusTransitionException;
import com.abdulrahman.university_management_system.enrollment.service.EnrollmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnrollmentController.class)
class EnrollmentStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnrollmentService enrollmentService;

    @Test
    void dropEnrollment_returns200() throws Exception {
        UUID id = UUID.randomUUID();

        when(enrollmentService.dropEnrollment(id))
                .thenReturn(response(id, EnrollmentStatus.DROPPED));

        mockMvc.perform(post("/api/v1/enrollments/{id}/drop", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("DROPPED"));
    }

    @Test
    void completeEnrollment_returns200() throws Exception {
        UUID id = UUID.randomUUID();

        when(enrollmentService.completeEnrollment(id))
                .thenReturn(response(id, EnrollmentStatus.COMPLETED));

        mockMvc.perform(post("/api/v1/enrollments/{id}/complete", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void dropEnrollment_returns404_whenEnrollmentDoesNotExist()
            throws Exception {
        UUID id = UUID.randomUUID();

        when(enrollmentService.dropEnrollment(id))
                .thenThrow(new EnrollmentNotFoundException(id));

        mockMvc.perform(post("/api/v1/enrollments/{id}/drop", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void completeEnrollment_returns409_whenTransitionIsInvalid()
            throws Exception {
        UUID id = UUID.randomUUID();

        when(enrollmentService.completeEnrollment(id))
                .thenThrow(new InvalidEnrollmentStatusTransitionException(id));

        mockMvc.perform(post("/api/v1/enrollments/{id}/complete", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    private EnrollmentResponse response(UUID id, EnrollmentStatus status) {
        Instant now = Instant.parse("2026-01-01T10:00:00Z");

        return new EnrollmentResponse(
                id,
                UUID.randomUUID(),
                UUID.randomUUID(),
                status,
                now,
                now,
                now
        );
    }
}
