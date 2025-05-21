package com.boot.cms.controller.mapview;

import com.boot.cms.dto.common.ApiResponseDto;
import com.boot.cms.service.mapview.MapViewProcessor;
import com.boot.cms.util.EscapeUtil;
import com.boot.cms.util.ResponseEntityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Map View", description = "Endpoint for dynamic map view data retrieval")
public class MapViewController {
    private static final Logger logger = LoggerFactory.getLogger(MapViewController.class);

    private final MapViewProcessor mapViewProcessor;
    private final ResponseEntityUtil responseEntityUtil;
    private final EscapeUtil escapeUtil;

    @Setter
    @Getter
    String errorMessage;

    @Operation(summary = "Retrieve dynamic map view data", description = "Processes dynamic view data based on report code, job type, and parameters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Data retrieved successfully", content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or missing rptCd parameter"),
            @ApiResponse(responseCode = "404", description = "No data found")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Request body containing report code and parameters", content = @Content(schema = @Schema(example = "{\"rptCd\": \"REPORT1\", \"jobGb\": \"GET\", \"empNo\": \"admin\", \"param1\": \"value1\"}")))
    @PostMapping("/call")
    public ResponseEntity<ApiResponseDto<List<Map<String, Object>>>> callDynamicView(
            @RequestBody Map<String, String> request
    ) {
        String rptCd = request.get("rptCd");
        String jobGb = request.getOrDefault("jobGb", "GET");
        String empNo = request.getOrDefault("empNo", "admin");

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