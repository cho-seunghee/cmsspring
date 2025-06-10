package com.boot.cms.controller.excelupload;

import com.boot.cms.config.AppConfig;
import com.boot.cms.dto.common.ApiResponseDto;
import com.boot.cms.entity.mapview.MapViewFileEntity;
import com.boot.cms.service.mapview.MapViewFileProcessor;
import com.boot.cms.service.mapview.MapViewProcessor;
import com.boot.cms.util.CommonApiResponses;
import com.boot.cms.util.EscapeUtil;
import com.boot.cms.util.MapViewParamsUtil;
import com.boot.cms.util.ResponseEntityUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

@RestController
@RequestMapping("api/excelupload/template")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "2.MAIN > 엑셀업로드템플릿관리", description = "엑셀업로드템플릿을 관리하는 API")
public class ExcelUploadTemplateController {
    private static final Logger logger = LoggerFactory.getLogger(ExcelUploadTemplateController.class);

    private final ResponseEntityUtil responseEntityUtil;
    private final MapViewProcessor mapViewProcessor;
    private final MapViewFileProcessor mapViewFileProcessor;
    private final EscapeUtil escapeUtil;
    private final MapViewParamsUtil mapViewParamsUtil;
    private final AppConfig.FileConfig fileConfig;

    @Setter
    @Getter
    String errorMessage;

    @CommonApiResponses
    @PostMapping("/filelist")
    public ResponseEntity<ApiResponseDto<List<Map<String, Object>>>> excelUploadTempFileList(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest
    ) {
        String rptCd = "EXCELUPLOADTEMPFILE";
        String jobGb = "GET";

        Claims claims = (Claims) httpRequest.getAttribute("user");
        String empNo = claims != null && claims.getSubject() != null ? claims.getSubject() : null;

        List<String> params = mapViewParamsUtil.getParams(request, escapeUtil);

        List<Map<String, Object>> unescapedResultList;
        try {
            unescapedResultList = mapViewFileProcessor.processDynamicView(rptCd, params, empNo, jobGb);
        } catch (IllegalArgumentException e) {
            errorMessage = "/filelist unescapedResultList = mapViewFileProcessor.processDynamicView(rptCd, params, empNo, jobGb);";
            logger.error(this.getErrorMessage(), e.getMessage(), e);
            return responseEntityUtil.okBodyEntity(null, "01", e.getMessage());
        }

        if (unescapedResultList.isEmpty()) {
            return responseEntityUtil.okBodyEntity(null, "01", "조회 결과가 없습니다.");
        }

        return responseEntityUtil.okBodyEntity(unescapedResultList);
    }

    @CommonApiResponses
    @PostMapping(value = "/filesave", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponseDto<List<MapViewFileEntity>>> excelUploadTempFileSave(
            String gubun,
            String fileId,
            String title,
            MultipartFile[] files,
            HttpServletRequest httpRequest) {

        // Validate required parameters
        if (gubun == null || gubun.trim().isEmpty() || title == null || title.trim().isEmpty()) {
            return responseEntityUtil.okBodyEntity(null, "01", "gubun and title are required.");
        }

        if (files == null || files.length == 0) {
            return responseEntityUtil.okBodyEntity(new ArrayList<>(), "00", "No files provided.");
        }

        if (files.length > fileConfig.getMaxFilesPerUpload()) {
            return responseEntityUtil.okBodyEntity(null, "01", "Too many files, maximum " + fileConfig.getMaxFilesPerUpload() + " allowed.");
        }

        String rptCd = "EXCELUPLOADTEMPFILETRAN";
        String jobGb = "SET";

        Claims claims = (Claims) httpRequest.getAttribute("user");
        String empNo = claims != null && claims.getSubject() != null ? claims.getSubject() : "admin";

        List<MapViewFileEntity> result = new ArrayList<>();
        try {
            // Process each file individually
            for (MultipartFile file : files) {
                String fileName = file.getOriginalFilename();
                if (fileName == null || fileName.trim().isEmpty()) {
                    logger.warn("Skipping file with empty name");
                    continue;
                }
                String fileType = fileName.substring(fileName.lastIndexOf(".") + 1);
                String fileSize = String.valueOf(file.getSize());

                List<Object> params = new ArrayList<>();
                params.add(escapeUtil.escape(gubun));
                params.add(escapeUtil.escape(fileId != null ? fileId : ""));
                params.add(escapeUtil.escape(title));
                params.add(escapeUtil.escape(empNo));
                params.add(escapeUtil.escape(fileName));
                params.add(escapeUtil.escape(fileType));
                params.add(escapeUtil.escape(fileSize));
                // Stream file content to avoid memory issues
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (InputStream inputStream = file.getInputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        baos.write(buffer, 0, bytesRead);
                    }
                }
                byte[] fileData = baos.toByteArray();
                if (fileData.length > fileConfig.getMaxFileSize()) {
                    throw new IllegalArgumentException("File size exceeds " + (fileConfig.getMaxFileSize() / (1024 * 1024)) + "MB limit");
                }
                params.add(fileData); // LONGBLOB data
                List<MapViewFileEntity> fileResult = mapViewFileProcessor.processFileUpload(rptCd, params, empNo, jobGb);

                result.addAll(fileResult);

                if (result.size() > fileConfig.getMaxResultSize()) {
                    logger.warn("Result size exceeds limit, truncating");
                    break;
                }
            }

            if (result.isEmpty()) {
                return responseEntityUtil.okBodyEntity(null, "01", "No files were processed successfully.");
            }

            return responseEntityUtil.okBodyEntity(result);
        } catch (IllegalArgumentException e) {
            errorMessage = "/filesave fileResult = mapViewFileProcessor.processFileUpload(rptCd, params, empNo, jobGb);";
            logger.error(this.getErrorMessage(), e.getMessage(), e);
            return responseEntityUtil.okBodyEntity(null, "01", "File upload failed: " + e.getMessage());
        } catch (Exception e) {
            errorMessage = "/filesave fileResult = mapViewFileProcessor.processFileUpload(rptCd, params, empNo, jobGb);";
            logger.error(this.getErrorMessage(), e.getMessage(), e);
            return responseEntityUtil.okBodyEntity(null, "01", "File upload failed: " + e.getMessage());
        }
    }

    @PostMapping(value = "/filedelete")
    public ResponseEntity<ApiResponseDto<List<MapViewFileEntity>>> excelUploadTempFileDelete(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {

        String gubun = (String) request.get("gubun");
        String fileId = (String) request.get("fileId");
        String title = (String) request.get("title");

        // Validate required parameters
        if (gubun == null || gubun.trim().isEmpty() || fileId == null || fileId.trim().isEmpty()) {
            return responseEntityUtil.okBodyEntity(null, "01", "gubun and fileId are required.");
        }

        if (!"D".equals(gubun)) {
            return responseEntityUtil.okBodyEntity(null, "01", "Invalid gubun value for deletion. Must be 'D'.");
        }

        String rptCd = "EXCELUPLOADTEMPFILETRAN";
        String jobGb = "SET";

        Claims claims = (Claims) httpRequest.getAttribute("user");
        String empNo = claims != null && claims.getSubject() != null ? claims.getSubject() : "admin";

        List<MapViewFileEntity> result = new ArrayList<>();
        try {
            // Deletion does not require file data, only metadata
            List<Object> params = new ArrayList<>();
            params.add(escapeUtil.escape(gubun));
            params.add(escapeUtil.escape(fileId));
            params.add(escapeUtil.escape(title));
            params.add(escapeUtil.escape(empNo));
            params.add(""); // pFILENM (empty for deletion)
            params.add(""); // pFILETYPE (empty for deletion)
            params.add("0"); // pFILESIZE (0 for deletion)
            params.add(new byte[0]); // pFILEDATA (empty byte array for deletion)

            List<MapViewFileEntity> fileResult = mapViewFileProcessor.processFileDelete(rptCd, params, empNo, jobGb);
            result.addAll(fileResult);

            if (result.isEmpty()) {
                return responseEntityUtil.okBodyEntity(null, "01", "File deletion failed: No results returned.");
            }

            return responseEntityUtil.okBodyEntity(result, "00", "File deleted successfully.");
        } catch (IllegalArgumentException e) {
            errorMessage = "/filedelete fileResult = mapViewFileProcessor.processFileUpload(rptCd, params, empNo, jobGb);";
            logger.error(this.getErrorMessage(), e.getMessage(), e);
            return responseEntityUtil.okBodyEntity(null, "01", "File deletion failed: " + e.getMessage());
        } catch (Exception e) {
            errorMessage = "/filedelete fileResult = mapViewFileProcessor.processFileUpload(rptCd, params, empNo, jobGb);";
            logger.error(this.getErrorMessage(), e.getMessage(), e);
            return responseEntityUtil.okBodyEntity(null, "01", "File deletion failed: " + e.getMessage());
        }
    }
}