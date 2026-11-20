package com.abdulrahman.university_management_system.enrollment.service;

import com.abdulrahman.university_management_system.common.pagination.PaginationValidator;
import com.abdulrahman.university_management_system.course.entity.Course;
import com.abdulrahman.university_management_system.course.exception.CourseNotFoundException;
import com.abdulrahman.university_management_system.course.repository.CourseRepository;
import com.abdulrahman.university_management_system.enrollment.dto.CreateEnrollmentRequest;
import com.abdulrahman.university_management_system.enrollment.dto.EnrollmentResponse;
import com.abdulrahman.university_management_system.enrollment.entity.Enrollment;
import com.abdulrahman.university_management_system.enrollment.entity.EnrollmentStatus;
import com.abdulrahman.university_management_system.enrollment.exception.EnrollmentAlreadyExistsException;
import com.abdulrahman.university_management_system.enrollment.exception.EnrollmentNotFoundException;
import com.abdulrahman.university_management_system.enrollment.exception.InvalidEnrollmentStatusTransitionException;
import com.abdulrahman.university_management_system.enrollment.repository.EnrollmentRepository;
import com.abdulrahman.university_management_system.student.entity.Student;
import com.abdulrahman.university_management_system.student.exception.StudentNotFoundException;
import com.abdulrahman.university_management_system.student.repository.StudentRepository;
import com.abdulrahman.university_management_system.user.entity.User;
import com.abdulrahman.university_management_system.user.entity.UserRole;
import com.abdulrahman.university_management_system.user.repository.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
public class EnrollmentService {

    private static final String STUDENT_COURSE_UNIQUE_CONSTRAINT =
            "uk_enrollments_student_course";

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "status", "enrolledAt", "createdAt", "updatedAt"
    );

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final PaginationValidator paginationValidator;

    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            PaginationValidator paginationValidator
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.paginationValidator = paginationValidator;
    }

    @Transactional
    public EnrollmentResponse createEnrollment(CreateEnrollmentRequest request) {
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new StudentNotFoundException(request.studentId()));

        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new CourseNotFoundException(request.courseId()));

        if (enrollmentRepository.existsByStudentIdAndCourseId(
                request.studentId(),
                request.courseId()
        )) {
            throw new EnrollmentAlreadyExistsException(
                    request.studentId(),
                    request.courseId()
            );
        }

        Enrollment enrollment = new Enrollment(student, course);

        try {
            Enrollment saved = enrollmentRepository.saveAndFlush(enrollment);
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateEnrollmentViolation(exception)) {
                throw new EnrollmentAlreadyExistsException(
                        request.studentId(),
                        request.courseId()
                );
            }

            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> getEnrollments(
            UUID studentId,
            UUID courseId,
            UUID userId,
            Pageable pageable
    ) {
        if (userId == null) {
            throw new AccessDeniedException("Access denied");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("Access denied"));

        if (user.getRole() == UserRole.STUDENT) {
            Student student = user.getStudent();

            if (student == null) {
                throw new AccessDeniedException("Access denied");
            }

            UUID ownStudentId = student.getId();

            if (studentId != null && !studentId.equals(ownStudentId)) {
                throw new AccessDeniedException("Access denied");
            }

            studentId = ownStudentId;
        } else if (user.getRole() != UserRole.ADMIN
                && user.getRole() != UserRole.STAFF) {
            throw new AccessDeniedException("Access denied");
        }

        Pageable validatedPageable =
                paginationValidator.validate(pageable, ALLOWED_SORT_FIELDS);

        Page<Enrollment> enrollments;

        if (studentId != null && courseId != null) {
            enrollments = enrollmentRepository.findAllByStudentIdAndCourseId(
                    studentId,
                    courseId,
                    validatedPageable
            );
        } else if (studentId != null) {
            enrollments = enrollmentRepository.findAllByStudentId(
                    studentId,
                    validatedPageable
            );
        } else if (courseId != null) {
            enrollments = enrollmentRepository.findAllByCourseId(
                    courseId,
                    validatedPageable
            );
        } else {
            enrollments = enrollmentRepository.findAll(validatedPageable);
        }

        return enrollments.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EnrollmentResponse getEnrollmentById(UUID id) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new EnrollmentNotFoundException(id));

        return toResponse(enrollment);
    }

    @Transactional
    public EnrollmentResponse dropEnrollment(UUID id) {
        return changeStatus(id, EnrollmentStatus.DROPPED);
    }

    @Transactional
    public EnrollmentResponse completeEnrollment(UUID id) {
        return changeStatus(id, EnrollmentStatus.COMPLETED);
    }

    private EnrollmentResponse changeStatus(UUID id, EnrollmentStatus newStatus) {
        Enrollment enrollment = enrollmentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EnrollmentNotFoundException(id));

        if (enrollment.getStatus() != EnrollmentStatus.ENROLLED) {
            throw new InvalidEnrollmentStatusTransitionException(id);
        }

        enrollment.setStatus(newStatus);
        enrollmentRepository.flush();

        return toResponse(enrollment);
    }

    private boolean isDuplicateEnrollmentViolation(
            DataIntegrityViolationException exception
    ) {
        if (exception.getCause()
                instanceof ConstraintViolationException constraintViolation) {

            return STUDENT_COURSE_UNIQUE_CONSTRAINT.equals(
                    constraintViolation.getConstraintName()
            );
        }

        return false;
    }

    private EnrollmentResponse toResponse(Enrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getStudent().getId(),
                enrollment.getCourse().getId(),
                enrollment.getStatus(),
                enrollment.getEnrolledAt(),
                enrollment.getCreatedAt(),
                enrollment.getUpdatedAt()
        );
    }
}
