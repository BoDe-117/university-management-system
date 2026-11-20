package com.abdulrahman.university_management_system.security.bootstrap;

import com.abdulrahman.university_management_system.user.dto.CreateUserRequest;
import com.abdulrahman.university_management_system.user.entity.UserRole;
import com.abdulrahman.university_management_system.user.repository.UserRepository;
import com.abdulrahman.university_management_system.user.service.UserService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminBootstrapService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final JdbcTemplate jdbcTemplate;

    public AdminBootstrapService(
            UserRepository userRepository,
            UserService userService,
            JdbcTemplate jdbcTemplate
    ) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public boolean createFirstAdmin(String username, String password) {
        // Serialize concurrent bootstrap attempts in PostgreSQL.
        jdbcTemplate.execute("SELECT pg_advisory_xact_lock(7041248119384821)");

        if (userRepository.existsByRole(UserRole.ADMIN)) {
            return false;
        }

        if (username == null || username.isBlank() || username.length() > 50) {
            throw new IllegalArgumentException("Admin username must be 1 to 50 characters");
        }

        if (password == null || password.isBlank() || password.length() < 12) {
            throw new IllegalArgumentException("Admin password must be at least 12 characters");
        }

        userService.createUser(
                new CreateUserRequest(username, password, UserRole.ADMIN, null)
        );

        return true;
    }
}
