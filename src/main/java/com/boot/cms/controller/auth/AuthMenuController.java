package com.boot.cms.controller.auth;

import com.boot.cms.dto.common.ApiResponseDto;
import com.boot.cms.mapper.auth.AuthMenuMapper;
import com.boot.cms.service.auth.AuthMenuService;
import com.boot.cms.util.ResponseEntityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
@Tag(name = "Menu Authorization", description = "Endpoint for retrieving user menu permissions")
public class AuthMenuController {
    private final AuthMenuMapper authMenuMapper;
    private final AuthMenuService authMenuService;
    private final ResponseEntityUtil responseEntityUtil;

    @Operation(summary = "Get user menu tree", description = "Retrieves the menu tree for a given user based on their user ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Menu tree retrieved successfully", content = @Content(schema = @Schema(implementation = ApiResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or missing userId parameter")
    })
    @PostMapping("menu")
    public ResponseEntity<ApiResponseDto<List<Map<String, Object>>>> menu(@RequestBody Map<String, String> request) {
        /* 2개이상 파라미터 검증
        return responseEntityUtil.handleListQuery(
            request,
            List.of("userId", "role"),
            params -> authMenuMapper.findByMenuAndRole(params.get("userId"), params.get("role")),
            "",
            ""
        );*/

        return responseEntityUtil.handleListQuery(
                request,
                List.of("userId"),
                params -> authMenuService.getMenuTree(params.get("userId")),
                "",
                ""
        );
    }

}
