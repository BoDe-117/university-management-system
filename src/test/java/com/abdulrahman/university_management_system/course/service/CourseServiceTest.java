package com.abdulrahman.university_management_system.course.service;

import com.abdulrahman.university_management_system.course.dto.CourseResponse;
import com.abdulrahman.university_management_system.course.dto.CreateCourseRequest;
import com.abdulrahman.university_management_system.course.entity.Course;
import com.abdulrahman.university_management_system.course.exception.CourseCodeAlreadyExistsException;
import com.abdulrahman.university_management_system.course.exception.CourseNotFoundException;
import com.abdulrahman.university_management_system.course.repository.CourseRepository;
import com.abdulrahman.university_management_system.program.entity.Program;
import com.abdulrahman.university_management_system.program.exception.ProgramNotFoundException;
import com.abdulrahman.university_management_system.program.repository.ProgramRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ProgramRepository programRepository;

    @InjectMocks
    private CourseService courseService;

    @Test
    void createCourse_savesAndReturnsCourse() {
        UUID programId = UUID.randomUUID();
        Program program = mock(Program.class);

        CreateCourseRequest request = new CreateCourseRequest(
                "CS101",
                "Introduction to Computing",
                3,
                programId
        );

        Course saved = new Course(
                "CS101",
                "Introduction to Computing",
                3,
                program
        );

        when(courseRepository.existsByCode("CS101")).thenReturn(false);
        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));
        when(courseRepository.saveAndFlush(any(Course.class)))
                .thenReturn(saved);

        when(program.getId()).thenReturn(programId);
        when(program.getCode()).thenReturn("BSC-CS");
        when(program.getName()).thenReturn("Computer Science");

        CourseResponse response = courseService.createCourse(request);

        assertThat(response.code()).isEqualTo("CS101");
        assertThat(response.name()).isEqualTo("Introduction to Computing");
        assertThat(response.credits()).isEqualTo(3);
        assertThat(response.programId()).isEqualTo(programId);
        assertThat(response.programCode()).isEqualTo("BSC-CS");
        assertThat(response.programName()).isEqualTo("Computer Science");

        verify(courseRepository).saveAndFlush(any(Course.class));
    }

    @Test
    void createCourse_rejectsDuplicateCode() {
        CreateCourseRequest request = new CreateCourseRequest(
                "CS101", "Introduction to Computing", 3, UUID.randomUUID()
        );

        when(courseRepository.existsByCode("CS101")).thenReturn(true);

        assertThatThrownBy(() -> courseService.createCourse(request))
                .isInstanceOf(CourseCodeAlreadyExistsException.class);

        verify(courseRepository, never()).saveAndFlush(any(Course.class));
    }

    @Test
    void createCourse_rejectsMissingProgram() {
        UUID programId = UUID.randomUUID();

        CreateCourseRequest request = new CreateCourseRequest(
                "CS101", "Introduction to Computing", 3, programId
        );

        when(courseRepository.existsByCode("CS101")).thenReturn(false);
        when(programRepository.findById(programId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.createCourse(request))
                .isInstanceOf(ProgramNotFoundException.class);

        verify(courseRepository, never()).saveAndFlush(any(Course.class));
    }

    @Test
    void createCourse_translatesDuplicateDatabaseConstraint() {
        UUID programId = UUID.randomUUID();
        Program program = mock(Program.class);

        CreateCourseRequest request = new CreateCourseRequest(
                "CS101", "Introduction to Computing", 3, programId
        );

        ConstraintViolationException constraintException =
                new ConstraintViolationException(
                        "duplicate code",
                        new SQLException(),
                        "uk_courses_code"
                );

        when(courseRepository.existsByCode("CS101")).thenReturn(false);
        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));
        when(courseRepository.saveAndFlush(any(Course.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicate code",
                        constraintException
                ));

        assertThatThrownBy(() -> courseService.createCourse(request))
                .isInstanceOf(CourseCodeAlreadyExistsException.class);
    }

    @Test
    void createCourse_rethrowsUnrelatedDatabaseError() {
        UUID programId = UUID.randomUUID();
        Program program = mock(Program.class);

        CreateCourseRequest request = new CreateCourseRequest(
                "CS101", "Introduction to Computing", 3, programId
        );

        DataIntegrityViolationException original =
                new DataIntegrityViolationException("other database error");

        when(courseRepository.existsByCode("CS101")).thenReturn(false);
        when(programRepository.findById(programId))
                .thenReturn(Optional.of(program));
        when(courseRepository.saveAndFlush(any(Course.class)))
                .thenThrow(original);

        assertThatThrownBy(() -> courseService.createCourse(request))
                .isSameAs(original);
    }

    @Test
    void getCourseById_rejectsMissingCourse() {
        UUID courseId = UUID.randomUUID();

        when(courseRepository.findById(courseId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getCourseById(courseId))
                .isInstanceOf(CourseNotFoundException.class);
    }

    @Test
    void getCoursesByProgramId_rejectsMissingProgram() {
        UUID programId = UUID.randomUUID();

        when(programRepository.existsById(programId)).thenReturn(false);

        assertThatThrownBy(() -> courseService.getCoursesByProgramId(programId))
                .isInstanceOf(ProgramNotFoundException.class);

        verify(courseRepository, never()).findAllByProgramId(programId);
    }
}
