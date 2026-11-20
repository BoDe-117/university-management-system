package com.abdulrahman.university_management_system.program.controller;

import com.abdulrahman.university_management_system.program.dto.CreateProgramRequest;
import com.abdulrahman.university_management_system.program.dto.ProgramResponse;
import com.abdulrahman.university_management_system.program.dto.UpdateProgramRequest;
import com.abdulrahman.university_management_system.program.service.ProgramService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
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

    @PutMapping("/{id}")
    public ResponseEntity<ProgramResponse> updateProgram(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProgramRequest request
    ) {
        ProgramResponse updated = programService.updateProgram(id, request);

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProgram(@PathVariable UUID id) {
        programService.deleteProgram(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProgramResponse> getProgramById(
            @PathVariable UUID id
    ) {
        ProgramResponse program = programService.getProgramById(id);

        return ResponseEntity.ok(program);
    }

    @GetMapping
    public ResponseEntity<Page<ProgramResponse>> getPrograms(
            @RequestParam(required = false) UUID departmentId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        Page<ProgramResponse> programs =
                programService.getPrograms(departmentId, pageable);

        return ResponseEntity.ok(programs);
    }
}
