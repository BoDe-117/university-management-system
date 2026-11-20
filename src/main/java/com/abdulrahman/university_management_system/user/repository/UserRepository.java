package com.abdulrahman.university_management_system.user.repository;

import com.abdulrahman.university_management_system.user.entity.User;
import com.abdulrahman.university_management_system.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByStudentId(UUID studentId);

    boolean existsByRole(UserRole role);
}
