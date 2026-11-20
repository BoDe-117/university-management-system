package com.abdulrahman.university_management_system.security.jwt;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String ISSUER = "university-management-system";

    private final SecretKey key = keyFrom(
            "test-secret-key-32-bytes-minimum-12345"
    );

    private final JwtEncoder encoder =
            new NimbusJwtEncoder(new ImmutableSecret<>(key));

    private final NimbusJwtDecoder decoder = createDecoder(key);

    private final JwtService jwtService = new JwtService(encoder, decoder);

    @Test
    void generatedToken_containsUserId() {
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateToken(userId);

        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    void tokenSignedWithAnotherKey_isRejected() {
        SecretKey otherKey = keyFrom(
                "another-test-key-32-bytes-minimum-123"
        );

        JwtService serviceWithOtherKey = new JwtService(
                encoder,
                createDecoder(otherKey)
        );

        String token = jwtService.generateToken(UUID.randomUUID());

        assertThatThrownBy(() -> serviceWithOtherKey.extractUserId(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void expiredToken_isRejected() {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(UUID.randomUUID().toString())
                .issuedAt(now.minusSeconds(600))
                .expiresAt(now.minusSeconds(300))
                .build();

        String token = encode(claims);

        assertThatThrownBy(() -> jwtService.extractUserId(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenWithoutExpiration_isRejected() {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .build();

        String token = encode(claims);

        assertThatThrownBy(() -> jwtService.extractUserId(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void tokenWithWrongIssuer_isRejected() {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("another-issuer")
                .subject(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();

        String token = encode(claims);

        assertThatThrownBy(() -> jwtService.extractUserId(token))
                .isInstanceOf(JwtException.class);
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return encoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

    private static NimbusJwtDecoder createDecoder(SecretKey secretKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));

        return decoder;
    }

    private static SecretKey keyFrom(String value) {
        return new SecretKeySpec(
                value.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
    }
}
