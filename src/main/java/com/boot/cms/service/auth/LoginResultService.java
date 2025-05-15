package com.boot.cms.service.auth;

import com.boot.cms.mapper.auth.LoginResultMapper;
import com.boot.cms.util.UserAgentUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoginResultService {
    private static final Logger logger = LoggerFactory.getLogger(LoginResultService.class);

    private final LoginResultMapper loginResultMapper;
    private final UserAgentUtil userAgentUtil;

    public Map<String, Object> callLoginProcedure(String empNo, String ip) {
        String userAgent = userAgentUtil.getUserAgent();
        String userConGb = userAgentUtil.getUserCongb();

        Map<String, Object> result;
        try {
            result = loginResultMapper.loginResultProcedure(empNo, ip, userConGb, userAgent);
        } catch (IllegalArgumentException e) {
            // 에러 로그 출력
            logger.error("프로시저 호출 중 오류가 발생했습니다. [프로시저명: {}], [입력값: empNo={}, ip={}, userConGb={}, userAgent={}], [에러메시지: {}]",
                    "loginResultProcedure", empNo, ip, userConGb, userAgent, e.getMessage(), e);

            // 사용자에게 표시할 새로운 예외 던지기
            throw new IllegalArgumentException("데이터베이스 처리 중 오류가 발생했습니다. 관리자에게 문의하십시오. 상세: " + e.getMessage());
        }

        return result;  // 프로시저에서 최종 SELECT로 출력된 결과를 반환
    }
}