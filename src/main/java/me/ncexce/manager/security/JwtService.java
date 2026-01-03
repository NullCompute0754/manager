package me.ncexce.manager.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.InitializingBean;

import java.util.Date;

@Service
public class JwtService implements InitializingBean {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey key;

    private static final long EXPIRATION = 1000 * 60 * 60 * 24; // 1 day

    @Override
    public void afterPropertiesSet() throws Exception {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalArgumentException("JWT secret cannot be null or empty");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String username, String role, String securityRole) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .claim("securityRole", securityRole)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(key)
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
