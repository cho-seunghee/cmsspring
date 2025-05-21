package com.boot.cms.controller.oper;

import com.boot.cms.dto.common.ApiResponseDto;
import com.boot.cms.service.mapview.MapViewProcessor;
import com.boot.cms.service.oper.OperAuthGroupMenuService;
import com.boot.cms.util.EscapeUtil;
import com.boot.cms.util.MapViewParamsUtil;
import com.boot.cms.util.ResponseEntityUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/oper/menuauthinfo")
@RequiredArgsConstructor
@Tag(name = "Operational Menu Authorization", description = "Endpoints for managing operational menu authorization data")
public class OperAuthGroupMenuController {

    private static final Logger logger = LoggerFactory.getLogger(OperAuthGroupMenuController.class);

    private final OperAuthGroupMenuService operAuthGroupMenuService;
    private final MapViewProcessor mapViewProcessor;
    private final ResponseEntityUtil responseEntityUtil;
    private final EscapeUtil escapeUtil;
    private final MapViewParamsUtil mapViewParamsUtil;

    @Setter
    @Getter
    String errorMessage;

    @Operation(summary = "List menu authorization data", description = "Retrieves menu authorization data for operational groups")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Data retrieved successfully", content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "No data found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Request body containing parameters", content = @Content(schema = @Schema(example = "{\"params\": {\"filter\": \"F\"}}")))
    @PostMapping("/list")
    public ResponseEntity<ApiResponseDto<List<Map<String, Object>>>> menuAuthList(
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
            errorMessage = "Error processing dynamic view: {}";
            logger.error(this.getErrorMessage(), e.getMessage(), e);
            return responseEntityUtil.okBodyEntity(null, "01", e.getMessage());
        }

        if (unescapedResultList.isEmpty()) {
            return responseEntityUtil.okBodyEntity(null, "01", "조회 결과가 없습니다.");
        }

        return responseEntityUtil.okBodyEntity(unescapedResultList);
    }

    @Operation(summary = "Save menu authorization data", description = "Saves or updates menu authorization data for operational groups")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Data saved successfully", content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "No data processed")
    })
    @SecurityRequirement(name = "bearerAuth")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Request body containing authorization data", content = @Content(schema = @Schema(example = "{\"params\": [{\"menuId\": \"MENU1\", \"auth\": \"READ\"}]}")))
    @PostMapping("/save")
    public ResponseEntity<ApiResponseDto<List<Map<String, Object>>>> menuAuthSave(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest
    ) {
        String rptCd = "OPERAUTHGROUPMENUTRAN";
        String jobGb = "SET";

        Claims claims = (Claims) httpRequest.getAttribute("user");
        String empNo = claims != null && claims.getSubject() != null ? claims.getSubject() : null;

        List<String> params = mapViewParamsUtil.getParams(request, escapeUtil);

        List<Map<String, Object>> unescapedResultList;
        try {
            unescapedResultList = mapViewProcessor.processDynamicView(rptCd, params, empNo, jobGb);
        } catch (IllegalArgumentException e) {
            errorMessage = "unescapedResultList = mapViewProcessor.processDynamicView(rptCd, params, empNo, jobGb);";
            logger.error(this.getErrorMessage(), e.getMessage(), e);
            return responseEntityUtil.okBodyEntity(null, "01", e.getMessage());
        }

        if (unescapedResultList.isEmpty()) {
            return responseEntityUtil.okBodyEntity(null, "01", "결과 없습니다.");
        }

        return responseEntityUtil.okBodyEntity(unescapedResultList);
    }
}