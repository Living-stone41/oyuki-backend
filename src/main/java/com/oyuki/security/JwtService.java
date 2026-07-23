package com.oyuki.security;

import com.oyuki.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final String jwtSecret;
    private final long jwtExpiration;

    public JwtService(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.expiration}") long jwtExpiration
    ) {
        this.jwtSecret = jwtSecret;
        this.jwtExpiration = jwtExpiration;
    }

    public String generateToken(User user) {

        Instant now = Instant.now();
        Instant expirationTime = now.plusMillis(jwtExpiration);
        

        return Jwts.builder()
        .subject(String.valueOf(user.getId()))
        .claim("role", user.getRole().name())
        .claim("fullName", user.getFullName())
        .claim("tokenVersion", user.getTokenVersion())
        .issuedAt(Date.from(now))
        .expiration(Date.from(expirationTime))
        .signWith(getSigningKey())
        .compact();
    }

    public Long extractUserId(String token) {
        String subject = extractClaims(token).getSubject();

        return Long.parseLong(subject);
    }

    public String extractRole(String token) {
        return extractClaims(token).get(
                "role",
                String.class
        );
    }

   public boolean isTokenValid(
        String token,
        User user
) {
    try {
        Claims claims = extractClaims(token);

        Long tokenUserId =
                Long.parseLong(claims.getSubject());

        Integer tokenVersion =
                claims.get("tokenVersion", Integer.class);

        return tokenUserId.equals(user.getId())
                && tokenVersion != null
                && tokenVersion.equals(user.getTokenVersion())
                && claims.getExpiration().after(new Date());

    } catch (JwtException |
             IllegalArgumentException exception) {

        return false;
    }
}
    public long getJwtExpiration() {
        return jwtExpiration;
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes =
                jwtSecret.getBytes(StandardCharsets.UTF_8);

        return Keys.hmacShaKeyFor(keyBytes);
    }
}