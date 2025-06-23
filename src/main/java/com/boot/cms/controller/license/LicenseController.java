package com.boot.cms.controller.license;

import com.boot.cms.dto.common.ApiResponseDto;
import com.boot.cms.model.license.LicenseInfo;
import com.boot.cms.service.license.LicenseService;
import com.boot.cms.util.ResponseEntityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("api/public")
@RequiredArgsConstructor
public class LicenseController {

    @Autowired
    private LicenseService licenseService;

    private final ResponseEntityUtil responseEntityUtil;

    // 1. 기존 HTML 템플릿 방식
    @GetMapping("/licenses")
    public String getLicenses(Model model) throws Exception {
        // 서비스 호출을 통해 데이터를 가져옵니다.
        List<LicenseInfo> licenses = licenseService.getLicenseInfo();
        // 데이터를 모델에 추가
        model.addAttribute("licenses", licenses);
        // "licenses.html" 템플릿 반환
        return "/license/licenses";
    }

    // 2. 추가된 JSON 응답 방식 (React 전용 엔드포인트)
    @GetMapping("/licenses/info")
    @ResponseBody
    public ResponseEntity<ApiResponseDto<List<LicenseInfo>>> getLicensesJson() throws Exception {
        // 서비스 호출
        List<LicenseInfo> licenseList = licenseService.getLicenseInfo();

        // 응답 데이터를 생성
        return responseEntityUtil.okBodyEntity(licenseList, "00", "Data fetched successfully.");
    }
}
