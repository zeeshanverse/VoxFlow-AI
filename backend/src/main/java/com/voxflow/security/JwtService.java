package com.voxflow.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMs = 1000L * 60 * 60 * 24;

    public JwtService(@Value("${voxflow.jwt.secret}") String secret) {
        if (secret.length() < 32) throw new IllegalArgumentException("JWT_SECRET must be at least 32 characters");
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generate(Long userId, String email) {
        Date now=new Date();
        return Jwts.builder().subject(email).claim("userId",userId)
                .issuedAt(now).expiration(new Date(now.getTime()+expirationMs))
                .signWith(key).compact();
    }

    public String extractEmail(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }

    public boolean valid(String token) {
        try { Jwts.parser().verifyWith(key).build().parseSignedClaims(token); return true; }
        catch (JwtException | IllegalArgumentException e) { return false; }
    }
}
