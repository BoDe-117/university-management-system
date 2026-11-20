package com.abdulrahman.university_management_system.security.service;

import com.abdulrahman.university_management_system.enrollment.entity.Enrollment;
import com.abdulrahman.university_management_system.enrollment.repository.EnrollmentRepository;
import com.abdulrahman.university_management_system.student.entity.Student;
import com.abdulrahman.university_management_system.user.entity.User;
import com.abdulrahman.university_management_system.user.entity.UserRole;
import com.abdulrahman.university_management_system.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnershipServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private OwnershipService ownershipService;

    @Test
    void ownsStudent_returnsTrueForLinkedStudent() {
        UUID userId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Student student = mock(Student.class);
        User user = new User("sara", "hash", UserRole.STUDENT, student);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(student.getId()).thenReturn(studentId);

        assertThat(ownershipService.ownsStudent(userId, studentId)).isTrue();
    }

    @Test
    void ownsStudent_returnsFalseForAnotherStudent() {
        UUID userId = UUID.randomUUID();
        UUID requestedStudentId = UUID.randomUUID();
        Student student = mock(Student.class);
        User user = new User("sara", "hash", UserRole.STUDENT, student);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(student.getId()).thenReturn(UUID.randomUUID());

        assertThat(ownershipService.ownsStudent(userId, requestedStudentId))
                .isFalse();
    }

    @Test
    void ownsStudent_returnsFalseForUnknownUser() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThat(ownershipService.ownsStudent(userId, UUID.randomUUID()))
                .isFalse();
    }

    @Test
    void ownsStudent_returnsFalseForAdmin() {
        UUID userId = UUID.randomUUID();
        User admin = new User("admin", "hash", UserRole.ADMIN, null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(admin));

        assertThat(ownershipService.ownsStudent(userId, UUID.randomUUID()))
                .isFalse();
    }

    @Test
    void ownsEnrollment_returnsTrueForStudentsOwnEnrollment() {
        UUID userId = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        Student student = mock(Student.class);
        Enrollment enrollment = mock(Enrollment.class);
        User user = new User("sara", "hash", UserRole.STUDENT, student);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(enrollmentRepository.findById(enrollmentId))
                .thenReturn(Optional.of(enrollment));
        when(enrollment.getStudent()).thenReturn(student);
        when(student.getId()).thenReturn(studentId);

        assertThat(ownershipService.ownsEnrollment(userId, enrollmentId))
                .isTrue();
    }

    @Test
    void ownsEnrollment_returnsFalseForAnotherStudentsEnrollment() {
        UUID userId = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();

        Student accountStudent = mock(Student.class);
        Student enrollmentStudent = mock(Student.class);
        Enrollment enrollment = mock(Enrollment.class);
        User user = new User("sara", "hash", UserRole.STUDENT, accountStudent);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(enrollmentRepository.findById(enrollmentId))
                .thenReturn(Optional.of(enrollment));
        when(enrollment.getStudent()).thenReturn(enrollmentStudent);
        when(accountStudent.getId()).thenReturn(UUID.randomUUID());
        when(enrollmentStudent.getId()).thenReturn(UUID.randomUUID());

        assertThat(ownershipService.ownsEnrollment(userId, enrollmentId))
                .isFalse();
    }

    @Test
    void ownsEnrollment_returnsFalseWhenEnrollmentDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();
        Student student = mock(Student.class);
        User user = new User("sara", "hash", UserRole.STUDENT, student);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(enrollmentRepository.findById(enrollmentId))
                .thenReturn(Optional.empty());

        assertThat(ownershipService.ownsEnrollment(userId, enrollmentId))
                .isFalse();
    }
}
