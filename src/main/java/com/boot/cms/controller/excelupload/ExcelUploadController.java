package com.boot.cms.controller.excelupload;

import com.boot.cms.config.AppConfig;
import com.boot.cms.dto.common.ApiResponseDto;
import com.boot.cms.service.excelupload.ExcelUploadService;
import com.boot.cms.util.CommonApiResponses;
import com.boot.cms.util.ResponseEntityUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 엑셀 업로드 컨트롤러 클래스
 * 엑셀 파일 업로드를 처리하는 API
 */
@RestController
@RequestMapping("api/excelupload")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "99.공통 > 엑셀업로드관리", description = "엑셀 파일 업로드를 관리하는 API")
public class ExcelUploadController {

    private static final Logger logger = LoggerFactory.getLogger(ExcelUploadController.class);

    private final ExcelUploadService excelUploadService;
    private final ResponseEntityUtil responseEntityUtil;
    private final AppConfig.FileConfig fileConfig;

    @Setter
    @Getter
    private String errorMessage;

    /**
     * 엑셀 파일 업로드 처리
     * @param rptCd 업로드 키코드
     * @param file 엑셀 파일
     * @param httpRequest HTTP 요청
     * @return 업로드 결과 응답
     */
    @CommonApiResponses
    @PostMapping(value = "/save", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> saveExcel(
            @RequestParam("rptCd") String rptCd,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {

        // 필수 파라미터 검증
        if (rptCd == null || rptCd.trim().isEmpty()) {
            return responseEntityUtil.okBodyEntity(null, "01", "업로드 키코드(rptCd)가 필요합니다.");
        }

        if (file == null || file.isEmpty()) {
            return responseEntityUtil.okBodyEntity(null, "01", "엑셀 파일(file)이 필요합니다.");
        }

        // 파일 크기 검증
        if (file.getSize() > fileConfig.getMaxFileSize()) {
            return responseEntityUtil.okBodyEntity(null, "01", "파일 크기가 " + (fileConfig.getMaxFileSize() / (1024 * 1024)) + "MB 제한을 초과했습니다.");
        }

        // 엑셀 파일 형식 검증 및 Workbook 생성
        XSSFWorkbook workbook = null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             InputStream inputStream = file.getInputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            byte[] fileData = baos.toByteArray();

            // 엑셀 파일인지 확인 (XSSFWorkbook으로 파싱 시도)
            try {
                workbook = new XSSFWorkbook(new ByteArrayInputStream(fileData));
            } catch (IOException e) {
                errorMessage = "유효하지 않은 엑셀 파일 형식입니다.";
                logger.error(errorMessage, e.getMessage(), e);
                return responseEntityUtil.okBodyEntity(null, "01", errorMessage);
            }
        } catch (IOException e) {
            errorMessage = "파일 데이터 변환 중 오류";
            logger.error(errorMessage, e.getMessage(), e);
            return responseEntityUtil.okBodyEntity(null, "01", "파일 처리 중 오류: " + e.getMessage());
        }

        // 사용자 정보 추출
        Claims claims = (Claims) httpRequest.getAttribute("user");
        String empNo = claims != null && claims.getSubject() != null ? claims.getSubject() : "admin";
        String empNm = claims != null && claims.get("empNm", String.class) != null ? claims.get("empNm", String.class) : "";

        // 비동기적으로 엑셀 업로드 처리
        // 비동기적으로 엑셀 업로드 처리
        try {
            excelUploadService.asyncExcelUpload(rptCd, workbook, empNo, empNm);
        } catch (Exception e) {
            errorMessage = "비동기 엑셀 업로드 처리 중 오류: " + rptCd; // 서비스와 동일한 메시지로 변경
            logger.error(errorMessage, e.getMessage(), e);
            return responseEntityUtil.okBodyEntity(null, "01", errorMessage);
        } finally {
            // Workbook 자원 해제
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (Exception e) {
                    logger.error("Workbook 닫기 실패", e);
                }
            }
        }

        // 성공 응답 생성 (클라이언트에 즉시 응답)
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("success", true);
        responseData.put("message", "파일은 정상적으로 등록되었습니다.");

        return responseEntityUtil.okBodyEntity(responseData);
    }
}