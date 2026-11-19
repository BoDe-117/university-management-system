package com.abdulrahman.university_management_system.user.service;

import com.abdulrahman.university_management_system.student.entity.Student;
import com.abdulrahman.university_management_system.student.exception.StudentNotFoundException;
import com.abdulrahman.university_management_system.student.repository.StudentRepository;
import com.abdulrahman.university_management_system.user.dto.CreateUserRequest;
import com.abdulrahman.university_management_system.user.dto.UserResponse;
import com.abdulrahman.university_management_system.user.entity.User;
import com.abdulrahman.university_management_system.user.entity.UserRole;
import com.abdulrahman.university_management_system.user.exception.InvalidPasswordException;
import com.abdulrahman.university_management_system.user.exception.InvalidUserStudentAssociationException;
import com.abdulrahman.university_management_system.user.exception.StudentAlreadyHasAccountException;
import com.abdulrahman.university_management_system.user.exception.UsernameAlreadyExistsException;
import com.abdulrahman.university_management_system.user.repository.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
public class UserService {

    private static final String USERNAME_CONSTRAINT = "uk_users_username";
    private static final String STUDENT_CONSTRAINT = "uk_users_student";
    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            StudentRepository studentRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (request.password() == null
                || request.password().getBytes(StandardCharsets.UTF_8).length
                > BCRYPT_MAX_BYTES) {
            throw new InvalidPasswordException(
                    "Password is required and must not exceed 72 UTF-8 bytes"
            );
        }

        if (request.role() == null) {
            throw new InvalidUserStudentAssociationException("Role is required");
        }

        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException(request.username());
        }

        Student student = null;

        if (request.role() == UserRole.STUDENT) {
            if (request.studentId() == null) {
                throw new InvalidUserStudentAssociationException(
                        "Student ID is required for a STUDENT account"
                );
            }

            student = studentRepository.findById(request.studentId())
                    .orElseThrow(() -> new StudentNotFoundException(request.studentId()));

            if (userRepository.existsByStudentId(request.studentId())) {
                throw new StudentAlreadyHasAccountException(request.studentId());
            }
        } else if (request.studentId() != null) {
            throw new InvalidUserStudentAssociationException(
                    "Student ID is only allowed for a STUDENT account"
            );
        }

        String passwordHash = passwordEncoder.encode(request.password());

        User user = new User(
                request.username(),
                passwordHash,
                request.role(),
                student
        );

        try {
            User saved = userRepository.saveAndFlush(user);
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            if (hasConstraint(exception, USERNAME_CONSTRAINT)) {
                throw new UsernameAlreadyExistsException(request.username());
            }

            if (hasConstraint(exception, STUDENT_CONSTRAINT)
                    && request.studentId() != null) {
                throw new StudentAlreadyHasAccountException(request.studentId());
            }

            throw exception;
        }
    }

    private boolean hasConstraint(
            DataIntegrityViolationException exception,
            String constraintName
    ) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                    && constraintName.equals(violation.getConstraintName())) {
                return true;
            }

            if (cause instanceof PSQLException postgresException) {
                ServerErrorMessage details = postgresException.getServerErrorMessage();

                if (details != null && constraintName.equals(details.getConstraint())) {
                    return true;
                }
            }

            cause = cause.getCause();
        }

        return false;
    }

    private UserResponse toResponse(User user) {
        Student student = user.getStudent();

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                student != null ? student.getId() : null,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
