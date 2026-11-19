package com.abdulrahman.university_management_system.security.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordConfigTest {

    private final PasswordEncoder passwordEncoder =
            new PasswordConfig().passwordEncoder();

    @Test
    void encodedPasswordMatchesOnlyTheCorrectPassword() {
        String password = "ExamplePassword123!";

        String hash = passwordEncoder.encode(password);

        assertThat(hash).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, hash)).isTrue();
        assertThat(passwordEncoder.matches("WrongPassword123!", hash)).isFalse();
    }
}
