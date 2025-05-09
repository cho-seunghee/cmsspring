package com.boot.cms.controller.oper;

import com.boot.cms.dto.common.ApiResponse;
import com.boot.cms.service.mapview.MapViewProcessor;
import com.boot.cms.service.oper.OperAuthGroupMenuService;
import com.boot.cms.util.EscapeUtil;
import com.boot.cms.util.ResponseEntityUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/mapview/oper/menuauthinfo")
@RequiredArgsConstructor
public class OperAuthGroupMenuController {

    private static final Logger logger = LoggerFactory.getLogger(OperAuthGroupMenuController.class);

    private final OperAuthGroupMenuService operAuthGroupMenuService;
    private final MapViewProcessor mapViewProcessor;
    private final ResponseEntityUtil responseEntityUtil;
    private final EscapeUtil escapeUtil;

    @PostMapping("/list")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> menuAuthList(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest
    ) {
        String rptCd = "OPERAUTHGROUPMENU";
        String jobGb = "GET";

        Claims claims = (Claims) httpRequest.getAttribute("user");
        String empNo = claims != null && claims.getSubject() != null ? claims.getSubject() : null;

        List<String> params;
        Object paramsObj = request.get("params");
        if (paramsObj instanceof Map && !((Map<?, ?>) paramsObj).isEmpty()) {
            Map<String, String> paramsMap = (Map<String, String>) paramsObj;
            params = paramsMap.values().stream()
                    .map(escapeUtil::escape)
                    .collect(Collectors.toList());
        } else if (!request.isEmpty()) {
            params = request.entrySet().stream()
                    .map(entry -> escapeUtil.escape(String.valueOf(entry.getValue())))
                    .collect(Collectors.toList());
        } else {
            params = List.of(escapeUtil.escape("F"));
        }

        List<Map<String, Object>> unescapedResultList;
        try {
            unescapedResultList = operAuthGroupMenuService.processDynamicView(rptCd, params, empNo, jobGb);
        } catch (IllegalArgumentException e) {
            logger.error("Error processing dynamic view: {}", e.getMessage());
            return responseEntityUtil.okBodyEntity(null, "01", e.getMessage());
        }

        if (unescapedResultList.isEmpty()) {
            return responseEntityUtil.okBodyEntity(null, "01", "조회 결과 없습니다.");
        }

        return responseEntityUtil.okBodyEntity(unescapedResultList);
    }
}