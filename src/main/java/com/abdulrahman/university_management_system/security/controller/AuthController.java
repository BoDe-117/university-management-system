package com.abdulrahman.university_management_system.security.controller;

import com.abdulrahman.university_management_system.security.dto.LoginRequest;
import com.abdulrahman.university_management_system.security.dto.TokenResponse;
import com.abdulrahman.university_management_system.security.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        try {
            TokenResponse response = authService.login(request);

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(response);
        } catch (BadCredentialsException exception) {
            return ResponseEntity.status(401).build();
        }
    }
}
