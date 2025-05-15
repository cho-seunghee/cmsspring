package com.boot.cms.service.auth;

import com.boot.cms.entity.auth.LoginEntity;
import com.boot.cms.mapper.auth.LoginMapper;
import com.boot.cms.util.Sha256Util;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final LoginMapper loginMapper;
    private final RedisTemplate<String, LoginEntity> redisTemplate;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public LoginEntity loginCheck(String empNo, String empPwd) {
        String cacheKey = "user:" + empNo;

        // Redis 캐시 조회
        try {
            LoginEntity cachedUser = redisTemplate.opsForValue().get(cacheKey);
            if (cachedUser != null) {
                return cachedUser;
            }

        } catch (RedisConnectionFailureException e) {
            System.out.println("Redis connection failed, falling back to DB: " + e.getMessage());
        }

        // 비밀번호 SHA-256 암호화
        String encryptedPwd = Sha256Util.encrypt(empPwd);

        // DB 조회
        LoginEntity user = loginMapper.loginCheck(empNo, encryptedPwd);
        if (user != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, user, jwtExpiration / 1000, TimeUnit.SECONDS);
            } catch (RedisConnectionFailureException e) {
                System.out.println("Failed to cache user in Redis: " + e.getMessage());
            }
            return user;
        }

        return null;
    }
}