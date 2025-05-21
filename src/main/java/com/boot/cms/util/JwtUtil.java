package com.boot.cms.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    @Getter
    private final Key signingKey;
    @Getter
    private final long expirationTime;

    @Value("${COOKIE_SECURE:false}")
    private boolean cookieSecure;

    @Value("${COOKIE_SAMESITE:Lax}")
    private String cookieSameSite;

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
        jwtCookie.setMaxAge(token == null ? 0 : (int) (expirationTime / 1000));
        jwtCookie.setSecure(cookieSecure); // Set to true in production (HTTPS)
        jwtCookie.setAttribute("SameSite", cookieSameSite); // Required for cross-origin
        return jwtCookie;
    }

    // Deprecated: Kept for backward compatibility but not used with cookies
    @Deprecated
    public String getTokenFromHeader() {
        return null;
    }
}