package com.boot.cms.controller.auth;

import com.boot.cms.aspect.ClientIPAspect;
import com.boot.cms.dto.common.ApiResponse;
import com.boot.cms.service.auth.AuthService;
import com.boot.cms.util.JwtUtil;
import com.boot.cms.util.ResponseEntityUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final ResponseEntityUtil responseEntityUtil;

    @GetMapping("live")
    public ResponseEntity<ApiResponse<Map<String, Object>>> live(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = "extend", defaultValue = "false") boolean extend) {
        String token = jwtUtil.getTokenFromCookie(request);
        if (token == null) {
            return responseEntityUtil.errBodyEntity("Missing token", 401);
        }

        try {
            Claims claims = authService.validateToken(token);
            String empNo = claims.getSubject();
            String auth = claims.get("auth", String.class);
            String empNm = claims.get("empNm", String.class);

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("empNo", empNo);
            userInfo.put("empNm", empNm);
            userInfo.put("auth", auth);
            userInfo.put("ip", ClientIPAspect.getClientIP());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("user", userInfo);
            long expiresAt = claims.getExpiration().getTime() / 1000;
            responseData.put("expiresAt", expiresAt);

            if (extend) {
                String newToken = jwtUtil.generateToken(empNo, auth, empNm);
                Cookie jwtCookie = jwtUtil.createJwtCookie(newToken);
                response.addCookie(jwtCookie);

                Claims newClaims = Jwts.parserBuilder()
                        .setSigningKey(jwtUtil.getSigningKey())
                        .build()
                        .parseClaimsJws(newToken)
                        .getBody();
                responseData.put("expiresAt", newClaims.getExpiration().getTime() / 1000);
            }

            return responseEntityUtil.okBodyEntity(responseData);
        } catch (Exception e) {
            return responseEntityUtil.errBodyEntity("Invalid token: " + e.getMessage(), 401);
        }
    }

    @GetMapping("check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> check(HttpServletRequest request) {
        String token = jwtUtil.getTokenFromCookie(request);
        if (token == null) {
            return responseEntityUtil.errBodyEntity("Missing token", 401);
        }

        try {
            Claims claims = jwtUtil.validateToken(token);
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            return responseEntityUtil.okBodyEntity(responseData);
        } catch (Exception e) {
            return responseEntityUtil.errBodyEntity("Invalid token: " + e.getMessage(), 401);
        }
    }

    @PostMapping("logout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> logout(HttpServletResponse response) {
        Cookie jwtCookie = jwtUtil.createJwtCookie(null);
        jwtCookie.setMaxAge(0); // Override to expire immediately
        response.addCookie(jwtCookie);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("success", true);
        return responseEntityUtil.okBodyEntity(responseData);
    }
}