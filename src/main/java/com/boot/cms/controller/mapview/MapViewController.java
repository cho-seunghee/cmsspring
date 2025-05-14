package com.boot.cms.controller.mapview;

import com.boot.cms.dto.common.ApiResponse;
import com.boot.cms.service.mapview.MapViewProcessor;
import com.boot.cms.util.EscapeUtil;
import com.boot.cms.util.ResponseEntityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/mapview")
@RequiredArgsConstructor
public class MapViewController {

    private final MapViewProcessor mapViewProcessor;
    private final ResponseEntityUtil responseEntityUtil;
    private final EscapeUtil escapeUtil;

    @PostMapping("/call")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> callDynamicView(
            @RequestBody Map<String, String> request
    ) {
        // Extract rptCd and jobGb from request
        String rptCd = request.get("rptCd");
        String jobGb = request.getOrDefault("jobGb", "GET"); // Fallback to "GET" if not provided
        String empNo = request.getOrDefault("empNo", "admin"); // Hardcoded or from request

        // Validate rptCd
        if (rptCd == null || rptCd.isEmpty()) {
            return responseEntityUtil.okBodyEntity(null, "01", "파라미터가 잘못되어 있습니다.");
        }

        // Extract params (all key-value pairs except rptCd, jobGb, empNo)
        List<String> params = request.entrySet().stream()
                .filter(entry -> !List.of("rptCd", "jobGb", "empNo").contains(entry.getKey()))
                .map(entry -> escapeUtil.escape(entry.getValue()))
                .collect(Collectors.toList());

        // Process dynamic view
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
}