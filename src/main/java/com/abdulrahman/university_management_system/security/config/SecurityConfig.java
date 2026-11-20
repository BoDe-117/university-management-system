package com.abdulrahman.university_management_system.security.config;

import com.abdulrahman.university_management_system.security.jwt.JwtAuthenticationFilter;
import com.abdulrahman.university_management_system.security.jwt.JwtService;
import com.abdulrahman.university_management_system.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            UserRepository userRepository
    ) throws Exception {
        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter(jwtService, userRepository);

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
                        )
                        .accessDeniedHandler((request, response, exception) ->
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN)
                        )
                )
                .authorizeHttpRequests(authorize -> authorize

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/login"
                        ).permitAll()

                        // Only administrators can create user accounts.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/users"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/departments",
                                "/api/v1/departments/*",
                                "/api/v1/programs",
                                "/api/v1/programs/*",
                                "/api/v1/courses",
                                "/api/v1/courses/*"
                        ).hasAnyRole("ADMIN", "STAFF", "STUDENT")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/students"
                        ).hasAnyRole("ADMIN", "STAFF")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/students/*"
                        ).hasAnyRole("ADMIN", "STAFF", "STUDENT")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/enrollments",
                                "/api/v1/enrollments/*/drop"
                        ).hasAnyRole("ADMIN", "STAFF", "STUDENT")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/enrollments/*"
                        ).hasAnyRole("ADMIN", "STAFF", "STUDENT")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/enrollments/*/complete"
                        ).hasAnyRole("ADMIN", "STAFF")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/departments",
                                "/api/v1/programs",
                                "/api/v1/courses",
                                "/api/v1/students"
                        ).hasAnyRole("ADMIN", "STAFF")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/departments/*",
                                "/api/v1/programs/*",
                                "/api/v1/courses/*",
                                "/api/v1/students/*"
                        ).hasAnyRole("ADMIN", "STAFF")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/departments/*",
                                "/api/v1/programs/*",
                                "/api/v1/courses/*",
                                "/api/v1/students/*"
                        ).hasRole("ADMIN")

                        .anyRequest().denyAll()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
