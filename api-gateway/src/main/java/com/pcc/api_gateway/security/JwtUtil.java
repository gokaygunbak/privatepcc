package com.pcc.api_gateway.security; // Kendi paket ismine dikkat et

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtil {

    private final SecretKey key;

    public JwtUtil(@org.springframework.beans.factory.annotation.Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public void validateToken(String token) {
        Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }
}