package com.abdulrahman.university_management_system.course.controller;

import com.abdulrahman.university_management_system.course.dto.CourseResponse;
import com.abdulrahman.university_management_system.course.exception.CourseCodeAlreadyExistsException;
import com.abdulrahman.university_management_system.course.exception.CourseNotFoundException;
import com.abdulrahman.university_management_system.course.service.CourseService;
import com.abdulrahman.university_management_system.program.exception.ProgramNotFoundException;
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

@WebMvcTest(CourseController.class)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    @Test
    void createCourse_returns201_whenRequestIsValid() throws Exception {
        UUID courseId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();
        Instant timestamp = Instant.parse("2026-01-01T10:00:00Z");

        CourseResponse response = new CourseResponse(
                courseId,
                "CS101",
                "Introduction to Computing",
                3,
                programId,
                "BSC-CS",
                "Computer Science",
                timestamp,
                timestamp
        );

        when(courseService.createCourse(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "CS101",
                                  "name": "Introduction to Computing",
                                  "credits": 3,
                                  "programId": "%s"
                                }
                                """.formatted(programId)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location", "/api/v1/courses/" + courseId
                ))
                .andExpect(jsonPath("$.id").value(courseId.toString()))
                .andExpect(jsonPath("$.credits").value(3))
                .andExpect(jsonPath("$.programId").value(programId.toString()));
    }

    @Test
    void createCourse_returns409_whenCodeAlreadyExists() throws Exception {
        when(courseService.createCourse(any()))
                .thenThrow(new CourseCodeAlreadyExistsException("CS101"));

        mockMvc.perform(post("/api/v1/courses")
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
    void createCourse_returns404_whenProgramDoesNotExist() throws Exception {
        UUID programId = UUID.randomUUID();

        when(courseService.createCourse(any()))
                .thenThrow(new ProgramNotFoundException(programId));

        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "CS101",
                                  "name": "Computing",
                                  "credits": 3,
                                  "programId": "%s"
                                }
                                """.formatted(programId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void createCourse_returns400_whenFieldsAreMissing() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.code").exists())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.credits").exists())
                .andExpect(jsonPath("$.fieldErrors.programId").exists());
    }

    @Test
    void createCourse_returns400_whenCreditsAreBelowMinimum() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
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
    void createCourse_returns400_whenCreditsExceedMaximum() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "CS101",
                                  "name": "Computing",
                                  "credits": 13,
                                  "programId": "%s"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.credits").exists());
    }

    @Test
    void getCourseById_returns404_whenCourseDoesNotExist() throws Exception {
        UUID courseId = UUID.randomUUID();

        when(courseService.getCourseById(courseId))
                .thenThrow(new CourseNotFoundException(courseId));

        mockMvc.perform(get("/api/v1/courses/{id}", courseId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getCourses_returnsCoursesForProgram() throws Exception {
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

        when(courseService.getCoursesByProgramId(programId))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/courses")
                        .queryParam("programId", programId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("CS101"));
    }
}
