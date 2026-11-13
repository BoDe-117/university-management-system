package com.abdulrahman.university_management_system.course.controller;

import com.abdulrahman.university_management_system.course.dto.CourseResponse;
import com.abdulrahman.university_management_system.course.exception.CourseCodeAlreadyExistsException;
import com.abdulrahman.university_management_system.course.exception.CourseNotFoundException;
import com.abdulrahman.university_management_system.course.service.CourseService;
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

@WebMvcTest(CourseController.class)
class CourseUpdateDeleteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    @Test
    void updateCourse_returns200_whenRequestIsValid() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();
        Instant timestamp = Instant.parse("2026-01-01T10:00:00Z");

        CourseResponse response = new CourseResponse(
                courseId,
                "CS101",
                "Computing",
                3,
                programId,
                "BSC-CS",
                "Computer Science",
                timestamp,
                timestamp
        );

        when(courseService.updateCourse(eq(courseId), any()))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/courses/{id}", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "CS101",
                                  "name": "Computing",
                                  "credits": 3,
                                  "programId": "%s"
                                }
                                """.formatted(programId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(courseId.toString()))
                .andExpect(jsonPath("$.code").value("CS101"))
                .andExpect(jsonPath("$.credits").value(3));
    }

    @Test
    void updateCourse_returns404_whenCourseDoesNotExist() throws Exception {
        UUID courseId = UUID.randomUUID();

        when(courseService.updateCourse(eq(courseId), any()))
                .thenThrow(new CourseNotFoundException(courseId));

        mockMvc.perform(put("/api/v1/courses/{id}", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "CS101",
                                  "name": "Computing",
                                  "credits": 3,
                                  "programId": "%s"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateCourse_returns409_whenCodeAlreadyExists() throws Exception {
        UUID courseId = UUID.randomUUID();

        when(courseService.updateCourse(eq(courseId), any()))
                .thenThrow(new CourseCodeAlreadyExistsException("CS101"));

        mockMvc.perform(put("/api/v1/courses/{id}", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "CS101",
                                  "name": "Computing",
                                  "credits": 3,
                                  "programId": "%s"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void updateCourse_returns400_whenCreditsAreInvalid() throws Exception {
        mockMvc.perform(put("/api/v1/courses/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "CS101",
                                  "name": "Computing",
                                  "credits": 0,
                                  "programId": "%s"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.credits").exists());
    }

    @Test
    void deleteCourse_returns204_whenDeletionSucceeds() throws Exception {
        UUID courseId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/courses/{id}", courseId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(courseService).deleteCourse(courseId);
    }

    @Test
    void deleteCourse_returns404_whenCourseDoesNotExist() throws Exception {
        UUID courseId = UUID.randomUUID();

        doThrow(new CourseNotFoundException(courseId))
                .when(courseService).deleteCourse(courseId);

        mockMvc.perform(delete("/api/v1/courses/{id}", courseId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deleteCourse_returns400_whenIdIsInvalid() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
