package com.abdulrahman.university_management_system.user.controller;

import com.abdulrahman.university_management_system.student.controller.StudentController;
import com.abdulrahman.university_management_system.student.exception.StudentHasUserAccountException;
import com.abdulrahman.university_management_system.student.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentController.class)
class StudentAccountDeleteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @Test
    @WithMockUser
    void deleteStudent_returns409_whenStudentHasUserAccount()
            throws Exception {
        UUID id = UUID.randomUUID();

        doThrow(new StudentHasUserAccountException(id))
                .when(studentService).deleteStudent(id);

        mockMvc.perform(
                        delete("/api/v1/students/{id}", id)
                                .with(csrf())
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").exists());
    }
}
