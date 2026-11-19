package com.abdulrahman.university_management_system.enrollment.controller;

import com.abdulrahman.university_management_system.course.controller.CourseController;
import com.abdulrahman.university_management_system.course.exception.CourseHasAssociatedEnrollmentsException;
import com.abdulrahman.university_management_system.course.service.CourseService;
import com.abdulrahman.university_management_system.student.controller.StudentController;
import com.abdulrahman.university_management_system.student.exception.StudentHasAssociatedEnrollmentsException;
import com.abdulrahman.university_management_system.student.service.StudentService;
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

@WebMvcTest({StudentController.class, CourseController.class})
class EnrollmentDeleteProtectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean
    private CourseService courseService;

    @Test
    void deleteStudent_returns409_whenStudentHasEnrollments()
            throws Exception {
        UUID id = UUID.randomUUID();

        doThrow(new StudentHasAssociatedEnrollmentsException(id))
                .when(studentService).deleteStudent(id);

        mockMvc.perform(delete("/api/v1/students/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void deleteCourse_returns409_whenCourseHasEnrollments()
            throws Exception {
        UUID id = UUID.randomUUID();

        doThrow(new CourseHasAssociatedEnrollmentsException(id))
                .when(courseService).deleteCourse(id);

        mockMvc.perform(delete("/api/v1/courses/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").exists());
    }
}
