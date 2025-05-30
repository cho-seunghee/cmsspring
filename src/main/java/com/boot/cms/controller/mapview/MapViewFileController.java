package com.boot.cms.controller.mapview;

import com.boot.cms.dto.common.ApiResponseDto;
import com.boot.cms.entity.mapview.MapViewFileEntity;
import com.boot.cms.service.mapview.MapViewFileProcessor;
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
@RequestMapping("api/mapviewfile")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "9.테스트 > Map View File", description = "MAP VIEW FILE을 통한 프로시저 호출 테스트 API")
public class MapViewFileController {

    private static final Logger logger = LoggerFactory.getLogger(MapViewFileController.class);

    private final MapViewFileProcessor mapViewFileProcessor;
    private final ResponseEntityUtil responseEntityUtil;
    private final EscapeUtil escapeUtil;

    @Setter
    @Getter
    String errorMessage;

    @CommonApiResponses
    @PostMapping("/call")
    public ResponseEntity<ApiResponseDto<List<MapViewFileEntity>>> processDynamicFileView(
            @RequestBody Map<String, Object> request
    ) {
        String rptCd = (String) request.get("rptCd");
        String jobGb = (String) request.getOrDefault("jobGb", "SET");
        String empNo = (String) request.getOrDefault("empNo", "mapviewFile");

        if (rptCd == null || rptCd.isEmpty()) {
            return responseEntityUtil.okBodyEntity(null, "01", "rptCd parameter is required.");
        }

        // Extract parameters, supporting both String and byte[] (for file data)
        List<Object> params = request.entrySet().stream()
                .filter(entry -> !List.of("rptCd", "jobGb", "empNo").contains(entry.getKey()))
                .map(entry -> {
                    Object value = entry.getValue();
                    if (value instanceof String) {
                        return escapeUtil.escape((String) value);
                    } else {
                        return value; // Keep byte[] or other types as-is
                    }
                })
                .collect(Collectors.toList());

        logger.info("Processing dynamic file view: rptCd={}, jobGb={}, empNo={}, paramCount={}", rptCd, jobGb, empNo, params.size());

        List<MapViewFileEntity> result;
        try {
            if ("GET".equalsIgnoreCase(jobGb)) {
                result = mapViewFileProcessor.processFileRetrieval(rptCd, params, empNo, jobGb);
            } else {
                result = mapViewFileProcessor.processFileUpload(rptCd, params, empNo, jobGb);
            }
        } catch (IllegalArgumentException e) {
            errorMessage = "mapViewFileProcessor.processFile: ";
            logger.error(this.getErrorMessage(), e.getMessage(), e);
            return responseEntityUtil.okBodyEntity(null, "01", e.getMessage());
        }

        if (result.isEmpty()) {
            return responseEntityUtil.okBodyEntity(null, "01", "조회 결과가 없습니다.");
        }

        return responseEntityUtil.okBodyEntity(result);
    }
}