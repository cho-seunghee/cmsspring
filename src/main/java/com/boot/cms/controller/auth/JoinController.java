package com.boot.cms.controller.auth;

import com.boot.cms.dto.common.ApiResponseDto;
import com.boot.cms.service.mapview.MapViewProcessor;
import com.boot.cms.util.CommonApiResponses;
import com.boot.cms.util.EscapeUtil;
import com.boot.cms.util.MapViewParamsUtil;
import com.boot.cms.util.ResponseEntityUtil;
import com.boot.cms.util.Sha256Util;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "1.LOGIN > 회원가입", description = "회원가입 API")
public class JoinController {
    private static final Logger logger = LoggerFactory.getLogger(JoinController.class);
    // TODO: 로깅을 위한 Logger 인스턴스 생성

    private final MapViewProcessor mapViewProcessor;
    // TODO: 동적 뷰 처리를 위한 MapViewProcessor 의존성 주입
    private final ResponseEntityUtil responseEntityUtil;
    // TODO: 응답 엔티티 생성을 위한 유틸리티 클래스 의존성 주입
    private final EscapeUtil escapeUtil;
    // TODO: 입력 데이터 이스케이프 처리를 위한 유틸리티 클래스 의존성 주입
    private final MapViewParamsUtil mapViewParamsUtil;
    // TODO: 요청 파라미터 처리를 위한 유틸리티 클래스 의존성 주입

    @Setter
    @Getter
    String errorMessage;
    // TODO: 에러 메시지 저장 및 조회를 위한 필드

    @CommonApiResponses
    @PostMapping("/join/list")
    public ResponseEntity<ApiResponseDto<List<Map<String, Object>>>> joinList(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest
    ) {
        // TODO: 회원가입 목록 조회를 위한 POST 엔드포인트
        String rptCd = "USERJOIN";
        // TODO: 동적 뷰 처리를 위한 보고서 코드 설정
        String jobGb = "GET";
        // TODO: 작업 구분을 조회(GET)로 설정

        Claims claims = (Claims) httpRequest.getAttribute("user");
        // TODO: HTTP 요청에서 사용자 클레임 정보 추출
        String empNo = claims != null && claims.getSubject() != null ? claims.getSubject() : null;
        // TODO: 클레임에서 직원 번호 추출, 없으면 null
        empNo = "joinCheck";  // TODO: 인증 체크를 패스하기 위한 임시 직원 번호 설정

        List<String> params = mapViewParamsUtil.getParams(request, escapeUtil);
        // TODO: 요청 데이터에서 파라미터 추출 및 이스케이프 처리

        List<Map<String, Object>> unescapedResultList;
        // TODO: 처리 결과를 저장할 리스트 선언
        try {
            unescapedResultList = mapViewProcessor.processDynamicView(rptCd, params, empNo, jobGb);
            // TODO: 동적 뷰 처리 호출
        } catch (IllegalArgumentException e) {
            errorMessage = "/list unescapedResultList = mapViewProcessor.processDynamicView(rptCd, params, empNo, jobGb);";
            // TODO: 예외 발생 시 에러 메시지 설정
            logger.error(this.getErrorMessage(), e.getMessage(), e);
            // TODO: 에러 로깅
            return responseEntityUtil.okBodyEntity(null, "01", e.getMessage());
            // TODO: 예외 발생 시 에러 응답 반환
        }

        if (unescapedResultList.isEmpty()) {
            // TODO: 조회 결과가 비어 있는지 확인
            return responseEntityUtil.okBodyEntity(null, "01", "조회 결과가 없습니다.");
            // TODO: 결과가 없으면 에러 응답 반환
        }

        return responseEntityUtil.okBodyEntity(unescapedResultList);
        // TODO: 정상 조회 결과 반환
    }

    @CommonApiResponses
    @PostMapping("/join/save")
    public ResponseEntity<ApiResponseDto<List<Map<String, Object>>>> joinSave(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest
    ) {
        // TODO: 회원가입 정보 저장을 위한 POST 엔드포인트
        String rptCd = "USERJOINTRAN";
        // TODO: 저장 처리를 위한 보고서 코드 설정
        String jobGb = "SET";
        // TODO: 작업 구분을 저장(SET)으로 설정

        Claims claims = (Claims) httpRequest.getAttribute("user");
        // TODO: HTTP 요청에서 사용자 클레임 정보 추출
        String empNo = claims != null && claims.getSubject() != null ? claims.getSubject() : null;
        // TODO: 클레임에서 직원 번호 추출, 없으면 null
        empNo = "joinCheck";  // TODO: 인증 체크를 패스하기 위한 임시 직원 번호 설정

        // Encrypt pEMPPWD if it exists in the request, preserving its position
        if (request.containsKey("pEMPPWD") && request.get("pEMPPWD") != null) {
            // TODO: 요청에 비밀번호(pEMPPWD)가 포함되어 있는지 확인
            String encryptedPassword = Sha256Util.encrypt(request.get("pEMPPWD").toString());
            // TODO: 비밀번호를 SHA-256으로 암호화
            request.put("pEMPPWD", encryptedPassword);
            // TODO: 암호화된 비밀번호로 요청 데이터 갱신
        }

        List<String> params = mapViewParamsUtil.getParams(request, escapeUtil);
        // TODO: 요청 데이터에서 파라미터 추출 및 이스케이프 처리

        // 현재 로그인 중인 사번을 넣을 경우 //String empNo = claims != null && claims.getSubject() != null ? claims.getSubject() : null;
        // 프로시저에서는 제일 마지막에 empno 받는 파라미터를 넣어야 한다.
        //params.add(empNo);

        List<Map<String, Object>> unescapedResultList;
        // TODO: 처리 결과를 저장할 리스트 선언
        try {
            unescapedResultList = mapViewProcessor.processDynamicView(rptCd, params, empNo, jobGb);
            // TODO: 동적 뷰 처리 호출
        } catch (IllegalArgumentException e) {
            errorMessage = "/save unescapedResultList = mapViewProcessor.processDynamicView(rptCd, params, empNo, jobGb);";
            // TODO: 예외 발생 시 에러 메시지 설정
            logger.error(this.getErrorMessage(), e.getMessage(), e);
            // TODO: 에러 로깅
            return responseEntityUtil.okBodyEntity(null, "01", e.getMessage());
            // TODO: 예외 발생 시 에러 응답 반환
        }

        if (unescapedResultList.isEmpty()) {
            // TODO: 처리 결과가 비어 있는지 확인
            return responseEntityUtil.okBodyEntity(null, "01", "결과 없습니다.");
            // TODO: 결과가 없으면 에러 응답 반환
        }

        return responseEntityUtil.okBodyEntity(unescapedResultList);
        // TODO: 정상 처리 결과 반환
    }
}