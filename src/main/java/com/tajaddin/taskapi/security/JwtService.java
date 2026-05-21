package com.tajaddin.taskapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Issues and validates HS256 JWTs. The secret is a base64-encoded 256-bit key
 * supplied via app.jwt.secret. Subject is the user id; the email is carried as
 * a custom claim for convenience.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlMinutes;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.ttl-minutes}") long ttlMinutes) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.ttlMinutes = ttlMinutes;
    }

    public long getTtlSeconds() {
        return ttlMinutes * 60;
    }

    public String issue(Long userId, String email) {
        Instant now = Instant.now();
        Instant exp = now.plus(ttlMinutes, ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    /**
     * Parse and validate a token. Throws JwtException (runtime) if the token is
     * expired, malformed, or has a bad signature.
     */
    public Long parseUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.valueOf(claims.getSubject());
    }
}
