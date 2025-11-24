package com.uf.apigateway.JWT;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JWTUtil {

    @Value("${jwt.secret:default-secret-change-me-please-32-bytes-minimum!!!!!}")
    private String secret;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Safe parser: returns claims or null if token invalid/malformed
    public Claims getAllClaimsSafe(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isTokenExpired(final Claims claims) {
        if (claims == null) return true;
        Date exp = claims.getExpiration();
        if (exp == null) return true;
        return exp.before(new Date());
    }

    public boolean isInvalid(String token) {
        Claims claims = getAllClaimsSafe(token);
        return isTokenExpired(claims);
    }

}