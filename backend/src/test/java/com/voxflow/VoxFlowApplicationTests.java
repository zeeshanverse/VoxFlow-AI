package com.voxflow;

import com.voxflow.security.JwtService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VoxFlowApplicationTests {

    private static final String JWT_SECRET =
            "test-secret-that-is-at-least-32-characters-long";

    @Test
    void jwtTokenCanBeGeneratedAndValidated() {
        JwtService jwtService = new JwtService(JWT_SECRET);

        String token = jwtService.generate(
                1L,
                "test@voxflow.com"
        );

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(jwtService.valid(token));
        assertEquals(
                "test@voxflow.com",
                jwtService.extractEmail(token)
        );
    }

    @Test
    void tamperedJwtTokenIsRejected() {
        JwtService jwtService = new JwtService(JWT_SECRET);

        String token = jwtService.generate(
                1L,
                "test@voxflow.com"
        );

        String tamperedToken = token + "tampered";

        assertFalse(jwtService.valid(tamperedToken));
    }

    @Test
    void invalidJwtTokenIsRejected() {
        JwtService jwtService = new JwtService(JWT_SECRET);

        assertFalse(
                jwtService.valid("this-is-not-a-valid-jwt")
        );
    }

    @Test
    void shortJwtSecretIsRejected() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new JwtService("too-short-secret")
                );

        assertEquals(
                "JWT_SECRET must be at least 32 characters",
                exception.getMessage()
        );
    }
}