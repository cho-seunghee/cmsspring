package com.boot.cms.controller.mapview;

import com.boot.cms.dto.common.ApiResponse;
import com.boot.cms.service.mapview.MapViewProcessor;
import com.boot.cms.util.EscapeUtil;
import com.boot.cms.util.ResponseEntityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        String rptCd = request.get("rptCd");
        if (rptCd == null || rptCd.isEmpty()) {
            return responseEntityUtil.okBodyEntity(null, "01", "파라미터가 잘못되어 있습니다.");
        }

        // Extract and escape parameters excluding rptCd
        List<String> params = request.entrySet().stream()
                .filter(entry -> !"rptCd".equals(entry.getKey())) // rptCd 필터링
                .sorted(Map.Entry.comparingByKey())              // 키 기준 정렬
                .map(entry -> escapeUtil.escape(entry.getValue())) // Escape each parameter
                .collect(Collectors.toList());

        // Process dynamic view using MapViewProcessor
        List<Map<String, Object>> unescapedResultList;
        try {
            unescapedResultList = mapViewProcessor.processDynamicView(rptCd, params);
        } catch (IllegalArgumentException e) {
            return responseEntityUtil.okBodyEntity(null, "01", e.getMessage());
        }

        if (unescapedResultList.isEmpty()) {
            return responseEntityUtil.okBodyEntity(null, "01", "조회 결과 없습니다.");
        }

        return responseEntityUtil.okBodyEntity(unescapedResultList);
    }
}