package com.boot.cms.controller.mapview;

import com.boot.cms.dto.common.ApiResponseDto;
import com.boot.cms.service.mapview.MapViewProcessor;
import com.boot.cms.util.CommonApiResponses;
import com.boot.cms.util.EscapeUtil;
import com.boot.cms.util.ResponseEntityUtil;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/mapview")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "9.테스트 > Map View", description = "MAP VIEW 를 통한 프로시저 호출 테스트 API")
public class MapViewController {
    private static final Logger logger = LoggerFactory.getLogger(MapViewController.class);

    private final MapViewProcessor mapViewProcessor;
    private final ResponseEntityUtil responseEntityUtil;
    private final EscapeUtil escapeUtil;

    @Setter
    @Getter
    String errorMessage;

    @CommonApiResponses
    @PostMapping("/call")
    public ResponseEntity<ApiResponseDto<List<Map<String, Object>>>> callDynamicView(
            @RequestBody Map<String, String> request
    ) {
        String rptCd = request.get("rptCd");
        String jobGb = request.getOrDefault("jobGb", "GET");
        String empNo = request.getOrDefault("empNo", "mapview");

        if (rptCd == null || rptCd.isEmpty()) {
            return responseEntityUtil.okBodyEntity(null, "01", "파라미터가 잘못되어 있습니다.");
        }

        List<String> params = request.entrySet().stream()
                .filter(entry -> !List.of("rptCd", "jobGb", "empNo").contains(entry.getKey()))
                .map(entry -> escapeUtil.escape(entry.getValue()))
                .collect(Collectors.toList());

        List<Map<String, Object>> unescapedResultList;
        try {
            unescapedResultList = mapViewProcessor.processDynamicView(rptCd, params, empNo, jobGb);
        } catch (IllegalArgumentException e) {
            errorMessage = "mapViewProcessor.processDynamicView(rptCd, params, empNo, jobGb) :";
            logger.error(this.getErrorMessage(), e.getMessage(), e);
            return responseEntityUtil.okBodyEntity(null, "01", e.getMessage());
        }

        if (unescapedResultList.isEmpty()) {
            return responseEntityUtil.okBodyEntity(null, "01", "조회 결과가 없습니다.");
        }

        return responseEntityUtil.okBodyEntity(unescapedResultList);
    }
}