package com.boot.cms.service.auth;

import com.boot.cms.entity.auth.LoginEntity;
import com.boot.cms.mapper.auth.LoginMapper;
import com.boot.cms.util.Sha256Util;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final LoginMapper loginMapper;

    public LoginEntity loginCheck(String empNo, String empPwd) {
        // 비밀번호 SHA-256 암호화
        String encryptedPwd = Sha256Util.encrypt(empPwd);

        // DB 조회
        LoginEntity user = loginMapper.loginCheck(empNo, encryptedPwd);
        return user;
    }
}