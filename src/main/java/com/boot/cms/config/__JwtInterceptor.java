package com.boot.cms.config;

import com.boot.cms.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class __JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String token = jwtUtil.getTokenFromHeader();
        if (token == null) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "No token provided");
            return false;
        }

        try {
            Claims claims = jwtUtil.validateToken(token);
            request.setAttribute("user", claims);
            return true;
        } catch (Exception e) {
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
            return false;
        }
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(String.format("{\"success\": false, \"message\": \"%s\"}", message));
    }
}
