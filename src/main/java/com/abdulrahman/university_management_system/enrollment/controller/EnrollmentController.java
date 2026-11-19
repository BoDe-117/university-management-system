package com.abdulrahman.university_management_system.enrollment.controller;

import com.abdulrahman.university_management_system.enrollment.dto.CreateEnrollmentRequest;
import com.abdulrahman.university_management_system.enrollment.dto.EnrollmentResponse;
import com.abdulrahman.university_management_system.enrollment.service.EnrollmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public ResponseEntity<EnrollmentResponse> createEnrollment(
            @Valid @RequestBody CreateEnrollmentRequest request
    ) {
        EnrollmentResponse response = enrollmentService.createEnrollment(request);

        URI location = URI.create("/api/v1/enrollments/" + response.id());

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnrollmentResponse> getEnrollmentById(
            @PathVariable UUID id
    ) {
        EnrollmentResponse response = enrollmentService.getEnrollmentById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/drop")
    public ResponseEntity<EnrollmentResponse> dropEnrollment(
            @PathVariable UUID id
    ) {
        EnrollmentResponse response = enrollmentService.dropEnrollment(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<EnrollmentResponse> completeEnrollment(
            @PathVariable UUID id
    ) {
        EnrollmentResponse response = enrollmentService.completeEnrollment(id);
        return ResponseEntity.ok(response);
    }
}
