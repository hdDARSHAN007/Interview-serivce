package com.Darshan.interview_service.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;

    // Must be the same secret that user-service signs tokens with
    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String extractEmail(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    // Tokens made before roles existed have no role, so they count as STUDENT
    public String extractRole(String token) {
        Object role = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().get("role");
        return role == null ? "STUDENT" : role.toString();
    }

    public boolean isTokenValid(String token) {
        try {
            Date expiration = Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(token).getBody().getExpiration();
            return expiration.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}