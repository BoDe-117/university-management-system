package com.abdulrahman.university_management_system.security.jwt;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class JwtService {

    private static final String ISSUER = "university-management-system";
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(15);

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    public JwtService(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
    }

    public String generateToken(UUID userId) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plus(TOKEN_LIFETIME))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

    public UUID extractUserId(String token) {
        Jwt jwt = jwtDecoder.decode(token);

        if (jwt.getExpiresAt() == null || !ISSUER.equals(
                jwt.getClaimAsString("iss")
        )) {
            throw new JwtException("Invalid token");
        }

        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new JwtException("Invalid token subject", exception);
        }
    }
}
