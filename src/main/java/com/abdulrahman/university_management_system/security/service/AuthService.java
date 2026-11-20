package com.abdulrahman.university_management_system.security.service;

import com.abdulrahman.university_management_system.security.dto.LoginRequest;
import com.abdulrahman.university_management_system.security.dto.TokenResponse;
import com.abdulrahman.university_management_system.security.jwt.JwtService;
import com.abdulrahman.university_management_system.user.entity.User;
import com.abdulrahman.university_management_system.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
public class AuthService {

    private static final int BCRYPT_MAX_BYTES = 72;

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtService jwtService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        if (request.password() == null
                || request.password().getBytes(StandardCharsets.UTF_8).length
                > BCRYPT_MAX_BYTES) {
            throw new BadCredentialsException("Invalid username or password");
        }

        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        request.username(),
                        request.password()
                )
        );

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid username or password")
                );

        String token = jwtService.generateToken(user.getId());

        return new TokenResponse(token, "Bearer");
    }
}
