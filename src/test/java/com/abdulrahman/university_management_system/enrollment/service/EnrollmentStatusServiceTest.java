package com.abdulrahman.university_management_system.enrollment.service;

import com.abdulrahman.university_management_system.course.entity.Course;
import com.abdulrahman.university_management_system.course.repository.CourseRepository;
import com.abdulrahman.university_management_system.enrollment.dto.EnrollmentResponse;
import com.abdulrahman.university_management_system.enrollment.entity.Enrollment;
import com.abdulrahman.university_management_system.enrollment.entity.EnrollmentStatus;
import com.abdulrahman.university_management_system.enrollment.exception.EnrollmentNotFoundException;
import com.abdulrahman.university_management_system.enrollment.exception.InvalidEnrollmentStatusTransitionException;
import com.abdulrahman.university_management_system.enrollment.repository.EnrollmentRepository;
import com.abdulrahman.university_management_system.student.entity.Student;
import com.abdulrahman.university_management_system.student.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentStatusServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    @Test
    void dropEnrollment_changesEnrolledToDropped() {
        UUID id = UUID.randomUUID();
        Student student = mock(Student.class);
        Course course = mock(Course.class);
        Enrollment enrollment = new Enrollment(student, course);

        when(enrollmentRepository.findByIdForUpdate(id))
                .thenReturn(Optional.of(enrollment));

        EnrollmentResponse response = enrollmentService.dropEnrollment(id);

        assertThat(response.status()).isEqualTo(EnrollmentStatus.DROPPED);
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.DROPPED);
        verify(enrollmentRepository).flush();
    }

    @Test
    void completeEnrollment_changesEnrolledToCompleted() {
        UUID id = UUID.randomUUID();
        Student student = mock(Student.class);
        Course course = mock(Course.class);
        Enrollment enrollment = new Enrollment(student, course);

        when(enrollmentRepository.findByIdForUpdate(id))
                .thenReturn(Optional.of(enrollment));

        EnrollmentResponse response = enrollmentService.completeEnrollment(id);

        assertThat(response.status()).isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
        verify(enrollmentRepository).flush();
    }

    @Test
    void dropEnrollment_rejectsMissingEnrollment() {
        UUID id = UUID.randomUUID();

        when(enrollmentRepository.findByIdForUpdate(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.dropEnrollment(id))
                .isInstanceOf(EnrollmentNotFoundException.class);

        verify(enrollmentRepository, never()).flush();
    }

    @Test
    void completeEnrollment_rejectsMissingEnrollment() {
        UUID id = UUID.randomUUID();

        when(enrollmentRepository.findByIdForUpdate(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.completeEnrollment(id))
                .isInstanceOf(EnrollmentNotFoundException.class);

        verify(enrollmentRepository, never()).flush();
    }

    @Test
    void completeEnrollment_rejectsDroppedEnrollment() {
        UUID id = UUID.randomUUID();
        Enrollment enrollment = new Enrollment(
                mock(Student.class),
                mock(Course.class)
        );
        enrollment.setStatus(EnrollmentStatus.DROPPED);

        when(enrollmentRepository.findByIdForUpdate(id))
                .thenReturn(Optional.of(enrollment));

        assertThatThrownBy(() -> enrollmentService.completeEnrollment(id))
                .isInstanceOf(InvalidEnrollmentStatusTransitionException.class);

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.DROPPED);
        verify(enrollmentRepository, never()).flush();
    }

    @Test
    void dropEnrollment_rejectsCompletedEnrollment() {
        UUID id = UUID.randomUUID();
        Enrollment enrollment = new Enrollment(
                mock(Student.class),
                mock(Course.class)
        );
        enrollment.setStatus(EnrollmentStatus.COMPLETED);

        when(enrollmentRepository.findByIdForUpdate(id))
                .thenReturn(Optional.of(enrollment));

        assertThatThrownBy(() -> enrollmentService.dropEnrollment(id))
                .isInstanceOf(InvalidEnrollmentStatusTransitionException.class);

        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
        verify(enrollmentRepository, never()).flush();
    }
}
