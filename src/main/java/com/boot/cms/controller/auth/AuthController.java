package com.boot.cms.controller.auth;

import com.boot.cms.aspect.ClientIPAspect;
import com.boot.cms.dto.common.ApiResponse;
import com.boot.cms.service.auth.AuthService;
import com.boot.cms.util.JwtUtil;
import com.boot.cms.util.ResponseEntityUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
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
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "extend", defaultValue = "false") boolean extend) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return responseEntityUtil.errBodyEntity("Missing or invalid token", 401);
        }

        String token = authHeader.substring(7);

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
                responseData.put("token", newToken);
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
}