package com.abdulrahman.university_management_system.security.service;

import com.abdulrahman.university_management_system.enrollment.entity.Enrollment;
import com.abdulrahman.university_management_system.enrollment.repository.EnrollmentRepository;
import com.abdulrahman.university_management_system.student.entity.Student;
import com.abdulrahman.university_management_system.user.entity.User;
import com.abdulrahman.university_management_system.user.entity.UserRole;
import com.abdulrahman.university_management_system.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OwnershipService {

    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    public OwnershipService(
            UserRepository userRepository,
            EnrollmentRepository enrollmentRepository
    ) {
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public boolean ownsStudent(UUID userId, UUID studentId) {
        if (userId == null || studentId == null) {
            return false;
        }

        User user = userRepository.findById(userId).orElse(null);

        if (user == null || user.getRole() != UserRole.STUDENT) {
            return false;
        }

        Student student = user.getStudent();

        return student != null && studentId.equals(student.getId());
    }

    @Transactional(readOnly = true)
    public boolean ownsEnrollment(UUID userId, UUID enrollmentId) {
        if (userId == null || enrollmentId == null) {
            return false;
        }

        User user = userRepository.findById(userId).orElse(null);

        if (user == null || user.getRole() != UserRole.STUDENT) {
            return false;
        }

        Student accountStudent = user.getStudent();

        if (accountStudent == null) {
            return false;
        }

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElse(null);

        return enrollment != null
                && accountStudent.getId().equals(enrollment.getStudent().getId());
    }
}
