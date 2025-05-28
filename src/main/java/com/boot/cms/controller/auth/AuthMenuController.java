package com.boot.cms.controller.auth;

import com.boot.cms.dto.common.ApiResponseDto;
import com.boot.cms.mapper.auth.AuthMenuMapper;
import com.boot.cms.service.auth.AuthMenuService;
import com.boot.cms.util.CommonApiResponses;
import com.boot.cms.util.ResponseEntityUtil;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "1.LOGIN > 메뉴", description = "사용자에 대한 메뉴 리스트 API")
public class AuthMenuController {
    private final AuthMenuMapper authMenuMapper;
    private final AuthMenuService authMenuService;
    private final ResponseEntityUtil responseEntityUtil;

    @CommonApiResponses
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