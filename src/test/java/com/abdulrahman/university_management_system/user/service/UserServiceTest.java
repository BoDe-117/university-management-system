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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String PASSWORD = "ExamplePassword123!";

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void createStudentAccount_encodesPasswordAndLinksStudent() {
        UUID studentId = UUID.randomUUID();
        Student student = mock(Student.class);

        CreateUserRequest request = new CreateUserRequest(
                "sara", PASSWORD, UserRole.STUDENT, studentId
        );

        when(studentRepository.findById(studentId))
                .thenReturn(Optional.of(student));
        when(student.getId()).thenReturn(studentId);
        when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.createUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(savedUser.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(savedUser.getStudent()).isSameAs(student);
        assertThat(response.username()).isEqualTo("sara");
        assertThat(response.role()).isEqualTo(UserRole.STUDENT);
        assertThat(response.studentId()).isEqualTo(studentId);
    }

    @Test
    void createAdminAccount_doesNotRequireStudent() {
        CreateUserRequest request = new CreateUserRequest(
                "admin", PASSWORD, UserRole.ADMIN, null
        );

        when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.createUser(request);

        assertThat(response.role()).isEqualTo(UserRole.ADMIN);
        assertThat(response.studentId()).isNull();
        verifyNoInteractions(studentRepository);
    }

    @Test
    void createUser_rejectsDuplicateUsername() {
        CreateUserRequest request = new CreateUserRequest(
                "sara", PASSWORD, UserRole.ADMIN, null
        );

        when(userRepository.existsByUsername("sara")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void createStudentAccount_rejectsMissingStudentId() {
        CreateUserRequest request = new CreateUserRequest(
                "sara", PASSWORD, UserRole.STUDENT, null
        );

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(InvalidUserStudentAssociationException.class);

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void createStudentAccount_rejectsUnknownStudent() {
        UUID studentId = UUID.randomUUID();

        CreateUserRequest request = new CreateUserRequest(
                "sara", PASSWORD, UserRole.STUDENT, studentId
        );

        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(StudentNotFoundException.class);

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void createStudentAccount_rejectsStudentWithExistingAccount() {
        UUID studentId = UUID.randomUUID();

        CreateUserRequest request = new CreateUserRequest(
                "sara", PASSWORD, UserRole.STUDENT, studentId
        );

        when(studentRepository.findById(studentId))
                .thenReturn(Optional.of(mock(Student.class)));
        when(userRepository.existsByStudentId(studentId)).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(StudentAlreadyHasAccountException.class);

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void createAdminAccount_rejectsStudentId() {
        CreateUserRequest request = new CreateUserRequest(
                "admin", PASSWORD, UserRole.ADMIN, UUID.randomUUID()
        );

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(InvalidUserStudentAssociationException.class);

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void createUser_rejectsPasswordLongerThan72Utf8Bytes() {
        String password = "a".repeat(73);

        CreateUserRequest request = new CreateUserRequest(
                "admin", password, UserRole.ADMIN, null
        );

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(InvalidPasswordException.class);

        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void createUser_countsPasswordBytesNotOnlyCharacters() {
        String password = "ع".repeat(37);

        CreateUserRequest request = new CreateUserRequest(
                "admin", password, UserRole.ADMIN, null
        );

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(InvalidPasswordException.class);

        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }
}
