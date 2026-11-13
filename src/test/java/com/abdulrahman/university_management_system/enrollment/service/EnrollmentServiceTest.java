package com.abdulrahman.university_management_system.enrollment.service;

import com.abdulrahman.university_management_system.course.entity.Course;
import com.abdulrahman.university_management_system.course.exception.CourseNotFoundException;
import com.abdulrahman.university_management_system.course.repository.CourseRepository;
import com.abdulrahman.university_management_system.enrollment.dto.CreateEnrollmentRequest;
import com.abdulrahman.university_management_system.enrollment.dto.EnrollmentResponse;
import com.abdulrahman.university_management_system.enrollment.entity.Enrollment;
import com.abdulrahman.university_management_system.enrollment.entity.EnrollmentStatus;
import com.abdulrahman.university_management_system.enrollment.exception.EnrollmentAlreadyExistsException;
import com.abdulrahman.university_management_system.enrollment.exception.EnrollmentNotFoundException;
import com.abdulrahman.university_management_system.enrollment.repository.EnrollmentRepository;
import com.abdulrahman.university_management_system.student.entity.Student;
import com.abdulrahman.university_management_system.student.exception.StudentNotFoundException;
import com.abdulrahman.university_management_system.student.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    @Test
    void createEnrollment_returnsSavedEnrollment() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();
        Instant now = Instant.now();

        Student student = mock(Student.class);
        Course course = mock(Course.class);
        Enrollment saved = mock(Enrollment.class);

        when(studentRepository.findById(studentId))
                .thenReturn(Optional.of(student));
        when(courseRepository.findById(courseId))
                .thenReturn(Optional.of(course));
        when(enrollmentRepository.saveAndFlush(any(Enrollment.class)))
                .thenReturn(saved);

        when(saved.getId()).thenReturn(enrollmentId);
        when(saved.getStudent()).thenReturn(student);
        when(saved.getCourse()).thenReturn(course);
        when(saved.getStatus()).thenReturn(EnrollmentStatus.ENROLLED);
        when(saved.getEnrolledAt()).thenReturn(now);
        when(saved.getCreatedAt()).thenReturn(now);
        when(saved.getUpdatedAt()).thenReturn(now);
        when(student.getId()).thenReturn(studentId);
        when(course.getId()).thenReturn(courseId);

        EnrollmentResponse response = enrollmentService.createEnrollment(
                new CreateEnrollmentRequest(studentId, courseId)
        );

        assertThat(response.id()).isEqualTo(enrollmentId);
        assertThat(response.studentId()).isEqualTo(studentId);
        assertThat(response.courseId()).isEqualTo(courseId);
        assertThat(response.status()).isEqualTo(EnrollmentStatus.ENROLLED);

        verify(enrollmentRepository).saveAndFlush(any(Enrollment.class));
    }

    @Test
    void createEnrollment_rejectsMissingStudent() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        when(studentRepository.findById(studentId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.createEnrollment(
                new CreateEnrollmentRequest(studentId, courseId)
        )).isInstanceOf(StudentNotFoundException.class);

        verify(enrollmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void createEnrollment_rejectsMissingCourse() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        when(studentRepository.findById(studentId))
                .thenReturn(Optional.of(mock(Student.class)));
        when(courseRepository.findById(courseId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.createEnrollment(
                new CreateEnrollmentRequest(studentId, courseId)
        )).isInstanceOf(CourseNotFoundException.class);

        verify(enrollmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void createEnrollment_rejectsDuplicate() {
        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();

        when(studentRepository.findById(studentId))
                .thenReturn(Optional.of(mock(Student.class)));
        when(courseRepository.findById(courseId))
                .thenReturn(Optional.of(mock(Course.class)));
        when(enrollmentRepository.existsByStudentIdAndCourseId(
                studentId, courseId
        )).thenReturn(true);

        assertThatThrownBy(() -> enrollmentService.createEnrollment(
                new CreateEnrollmentRequest(studentId, courseId)
        )).isInstanceOf(EnrollmentAlreadyExistsException.class);

        verify(enrollmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void getEnrollmentById_rejectsMissingEnrollment() {
        UUID id = UUID.randomUUID();

        when(enrollmentRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.getEnrollmentById(id))
                .isInstanceOf(EnrollmentNotFoundException.class);
    }
}
