package com.boot.cms.controller.oper;

import com.boot.cms.dto.common.ApiResponse;
import com.boot.cms.service.mapview.MapViewProcessor;
import com.boot.cms.util.EscapeUtil;
import com.boot.cms.util.MapViewParamsUtil;
import com.boot.cms.util.ResponseEntityUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
//@RequestMapping("api/public/oper/menumng") // postman 테스트 시
@RequestMapping("api/oper/menumng")
@RequiredArgsConstructor
public class OperMenuMngController {

    private final MapViewProcessor mapViewProcessor;
    private final ResponseEntityUtil responseEntityUtil;
    private final EscapeUtil escapeUtil;
    private final MapViewParamsUtil mapViewParamsUtil;

    @PostMapping("/list")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> menuMngList(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest
    ) {
        String rptCd = "OPERMENUMNG";
        String jobGb = "GET";

        Claims claims = (Claims) httpRequest.getAttribute("user");
        String empNo = claims != null && claims.getSubject() != null ? claims.getSubject() : null;
//        empNo = "test"; // postman 테스트 시

        List<String> params = mapViewParamsUtil.getParams(request, escapeUtil);

        List<Map<String, Object>> unescapedResultList;
        try {
            unescapedResultList = mapViewProcessor.processDynamicView(rptCd, params, empNo, jobGb);
        } catch (IllegalArgumentException e) {
            return responseEntityUtil.okBodyEntity(null, "01", e.getMessage());
        }

        if (unescapedResultList.isEmpty()) {
            return responseEntityUtil.okBodyEntity(null, "01", "조회 결과가 없습니다.");
        }

        return responseEntityUtil.okBodyEntity(unescapedResultList);
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> menuMngSave(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest
    ) {
        String rptCd = "OPERMENUMNGTRAN";
        String jobGb = "SET";

        Claims claims = (Claims) httpRequest.getAttribute("user");
        String empNo = claims != null && claims.getSubject() != null ? claims.getSubject() : null;
//        empNo = "test"; // postman 테스트 시

        List<String> params = mapViewParamsUtil.getParams(request, escapeUtil);

        List<Map<String, Object>> unescapedResultList;
        try {
            unescapedResultList = mapViewProcessor.processDynamicView(rptCd, params, empNo, jobGb);
        } catch (IllegalArgumentException e) {
            return responseEntityUtil.okBodyEntity(null, "01", e.getMessage());
        }

        if (unescapedResultList.isEmpty()) {
            return responseEntityUtil.okBodyEntity(null, "01", "결과 없습니다.");
        }

        return responseEntityUtil.okBodyEntity(unescapedResultList);
    }
}