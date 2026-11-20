package com.abdulrahman.university_management_system.enrollment.controller;

import com.abdulrahman.university_management_system.enrollment.dto.CreateEnrollmentRequest;
import com.abdulrahman.university_management_system.enrollment.dto.EnrollmentResponse;
import com.abdulrahman.university_management_system.enrollment.service.EnrollmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'STAFF') or "
                    + "(hasRole('STUDENT') and "
                    + "@ownershipService.ownsStudent(authentication.principal, #request.studentId()))"
    )
    public ResponseEntity<EnrollmentResponse> createEnrollment(
            @Valid @RequestBody CreateEnrollmentRequest request
    ) {
        EnrollmentResponse response = enrollmentService.createEnrollment(request);

        URI location = URI.create("/api/v1/enrollments/" + response.id());

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'STUDENT')")
    public ResponseEntity<Page<EnrollmentResponse>> getEnrollments(
            @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) UUID courseId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable,
            Authentication authentication
    ) {
        if (!(authentication.getPrincipal() instanceof UUID userId)) {
            throw new AccessDeniedException("Access denied");
        }

        Page<EnrollmentResponse> enrollments =
                enrollmentService.getEnrollments(
                        studentId,
                        courseId,
                        userId,
                        pageable
                );

        return ResponseEntity.ok(enrollments);
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'STAFF') or "
                    + "(hasRole('STUDENT') and "
                    + "@ownershipService.ownsEnrollment(authentication.principal, #id))"
    )
    public ResponseEntity<EnrollmentResponse> getEnrollmentById(
            @PathVariable UUID id
    ) {
        EnrollmentResponse response = enrollmentService.getEnrollmentById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/drop")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'STAFF') or "
                    + "(hasRole('STUDENT') and "
                    + "@ownershipService.ownsEnrollment(authentication.principal, #id))"
    )
    public ResponseEntity<EnrollmentResponse> dropEnrollment(
            @PathVariable UUID id
    ) {
        EnrollmentResponse response = enrollmentService.dropEnrollment(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<EnrollmentResponse> completeEnrollment(
            @PathVariable UUID id
    ) {
        EnrollmentResponse response = enrollmentService.completeEnrollment(id);
        return ResponseEntity.ok(response);
    }
}
