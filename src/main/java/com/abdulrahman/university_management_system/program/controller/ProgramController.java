package com.abdulrahman.university_management_system.program.controller;

import com.abdulrahman.university_management_system.program.dto.CreateProgramRequest;
import com.abdulrahman.university_management_system.program.dto.ProgramResponse;
import com.abdulrahman.university_management_system.program.service.ProgramService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/programs")
public class ProgramController {

    private final ProgramService programService;

    public ProgramController(ProgramService programService) {
        this.programService = programService;
    }

    @PostMapping
    public ResponseEntity<ProgramResponse> createProgram(
            @Valid @RequestBody CreateProgramRequest request
    ) {
        ProgramResponse created = programService.createProgram(request);

        URI location = URI.create(
                "/api/v1/programs/" + created.id()
        );

        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProgramResponse> getProgramById(
            @PathVariable UUID id
    ) {
        ProgramResponse program = programService.getProgramById(id);

        return ResponseEntity.ok(program);
    }

    @GetMapping
    public ResponseEntity<List<ProgramResponse>> getPrograms(
            @RequestParam(required = false) UUID departmentId
    ) {
        List<ProgramResponse> programs;

        if (departmentId == null) {
            programs = programService.getAllPrograms();
        } else {
            programs =
                    programService.getProgramsByDepartmentId(departmentId);
        }

        return ResponseEntity.ok(programs);
    }
}
