package com.boot.cms.controller.auth;

import com.boot.cms.dto.common.ApiResponse;
import com.boot.cms.mapper.auth.AuthMenuMapper;
import com.boot.cms.service.auth.AuthMenuService;
import com.boot.cms.util.ResponseEntityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class AuthMenuController {
    private final AuthMenuMapper authMenuMapper;
    private final AuthMenuService authMenuService;
    private final ResponseEntityUtil responseEntityUtil;

    @PostMapping("menu")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> menu(@RequestBody Map<String, String> request) {
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
