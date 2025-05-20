package com.boot.cms.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    private final Key signingKey;
    private final long expirationTime;

    public JwtUtil(@Value("${jwt.secret}") String secretKey, @Value("${jwt.expiration}") long expirationTime) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secretKey);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Base64 encoded jwt.secret", e);
        }
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("Secret key must be at least 256 bits");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationTime = expirationTime;
    }

    public Key getSigningKey() {
        return signingKey;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public String generateToken(String empNo, String auth, String empNm) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);
        return Jwts.builder()
                .setSubject(empNo)
                .claim("auth", auth)
                .claim("empNm", empNm)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    public Claims validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            throw new RuntimeException("Invalid or expired token: " + e.getMessage(), e);
        }
    }

    public String getTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public Cookie createJwtCookie(String token) {
        Cookie jwtCookie = new Cookie("jwt_token", token);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge((int) (expirationTime / 1000)); // Match token expiration
        jwtCookie.setSecure(true); // Set to true in production (HTTPS)
        jwtCookie.setAttribute("SameSite", "Lax");
        return jwtCookie;
    }

    // Deprecated: Kept for backward compatibility but not used with cookies
    @Deprecated
    public String getTokenFromHeader() {
        // Existing header-based method (not used)
        return null;
    }
}