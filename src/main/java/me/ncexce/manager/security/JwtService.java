package me.ncexce.manager.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    private final SecretKey key = Keys.hmacShaKeyFor(
        secret.getBytes()
    );
    private static final long EXPIRATION = 1000 * 60 * 60 * 24; // 1 day

    public String generateToken(String username, String role, String securityRole) {

        return Jwts.builder()
                .setSubject(username)       // NOT subject(), use setSubject()
                .claim("role", role)
                .claim("securityRole", securityRole)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(key)              // works in 0.11+ with SecretKey
                .compact();
    }

    public Claims validateToken(String token) throws JwtException, ExpiredJwtException {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getUsernameFromToken(String token) {
        return validateToken(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return validateToken(token).get("role").toString();
    }
}
