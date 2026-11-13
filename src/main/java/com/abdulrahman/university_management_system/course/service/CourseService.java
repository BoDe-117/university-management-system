package com.abdulrahman.university_management_system.course.service;

import com.abdulrahman.university_management_system.course.dto.CourseResponse;
import com.abdulrahman.university_management_system.course.dto.CreateCourseRequest;
import com.abdulrahman.university_management_system.course.dto.UpdateCourseRequest;
import com.abdulrahman.university_management_system.course.entity.Course;
import com.abdulrahman.university_management_system.course.exception.CourseCodeAlreadyExistsException;
import com.abdulrahman.university_management_system.course.exception.CourseNotFoundException;
import com.abdulrahman.university_management_system.course.repository.CourseRepository;
import com.abdulrahman.university_management_system.program.entity.Program;
import com.abdulrahman.university_management_system.program.exception.ProgramNotFoundException;
import com.abdulrahman.university_management_system.program.repository.ProgramRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CourseService {

    private static final String COURSE_CODE_UNIQUE_CONSTRAINT =
            "uk_courses_code";

    private final CourseRepository courseRepository;
    private final ProgramRepository programRepository;

    public CourseService(
            CourseRepository courseRepository,
            ProgramRepository programRepository
    ) {
        this.courseRepository = courseRepository;
        this.programRepository = programRepository;
    }

    @Transactional
    public CourseResponse createCourse(CreateCourseRequest request) {
        if (courseRepository.existsByCode(request.code())) {
            throw new CourseCodeAlreadyExistsException(request.code());
        }

        Program program = programRepository.findById(request.programId())
                .orElseThrow(() ->
                        new ProgramNotFoundException(request.programId())
                );

        Course course = new Course(
                request.code(),
                request.name(),
                request.credits(),
                program
        );

        try {
            Course saved = courseRepository.saveAndFlush(course);
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateCodeViolation(exception)) {
                throw new CourseCodeAlreadyExistsException(request.code());
            }

            throw exception;
        }
    }

    @Transactional
    public CourseResponse updateCourse(UUID id, UpdateCourseRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));

        if (courseRepository.existsByCodeAndIdNot(request.code(), id)) {
            throw new CourseCodeAlreadyExistsException(request.code());
        }

        Program program = programRepository.findById(request.programId())
                .orElseThrow(() ->
                        new ProgramNotFoundException(request.programId())
                );

        course.setCode(request.code());
        course.setName(request.name());
        course.setCredits(request.credits());
        course.setProgram(program);

        try {
            Course saved = courseRepository.saveAndFlush(course);
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateCodeViolation(exception)) {
                throw new CourseCodeAlreadyExistsException(request.code());
            }

            throw exception;
        }
    }

    @Transactional
    public void deleteCourse(UUID id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));

        courseRepository.delete(course);
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourseById(UUID id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));

        return toResponse(course);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getAllCourses() {
        return courseRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getCoursesByProgramId(UUID programId) {
        if (!programRepository.existsById(programId)) {
            throw new ProgramNotFoundException(programId);
        }

        return courseRepository.findAllByProgramId(programId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private boolean isDuplicateCodeViolation(
            DataIntegrityViolationException exception
    ) {
        if (exception.getCause()
                instanceof ConstraintViolationException constraintViolation) {

            return COURSE_CODE_UNIQUE_CONSTRAINT.equals(
                    constraintViolation.getConstraintName()
            );
        }

        return false;
    }

    private CourseResponse toResponse(Course course) {
        Program program = course.getProgram();

        return new CourseResponse(
                course.getId(),
                course.getCode(),
                course.getName(),
                course.getCredits(),
                program.getId(),
                program.getCode(),
                program.getName(),
                course.getCreatedAt(),
                course.getUpdatedAt()
        );
    }
}
