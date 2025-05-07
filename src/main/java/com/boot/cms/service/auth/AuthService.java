package com.boot.cms.service.auth;

import com.boot.cms.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtUtil jwtUtil;

    public Claims validateToken(String token) {
        return jwtUtil.validateToken(token);
    }
}