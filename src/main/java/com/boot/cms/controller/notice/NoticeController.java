package com.boot.cms.controller.notice;

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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

@RestController
@RequestMapping("api/notice")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "2.MAIN > 공지사항관리", description = "공지사항을 관리하는 API")
public class NoticeController {
    private static final Logger logger = LoggerFactory.getLogger(NoticeController.class);

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
    @PostMapping("/list")
    public ResponseEntity<ApiResponseDto<List<Map<String, Object>>>> noticeList(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest
    ) {
        String rptCd = "NOTICE";
        String jobGb = "GET";

        Claims claims = (Claims) httpRequest.getAttribute("user");
        String empNo = claims != null && claims.getSubject() != null ? claims.getSubject() : null;

        List<String> params = mapViewParamsUtil.getParams(request, escapeUtil);

        List<Map<String, Object>> unescapedResultList;
        try {
            unescapedResultList = mapViewProcessor.processDynamicView(rptCd, params, empNo, jobGb);
        } catch (IllegalArgumentException e) {
            errorMessage = "/list unescapedResultList = mapViewProcessor.processDynamicView(rptCd, params, empNo, jobGb);";
            logger.error(this.getErrorMessage(), e.getMessage(), e);
            return responseEntityUtil.okBodyEntity(null, "01", e.getMessage());
        }

        if (unescapedResultList.isEmpty()) {
            return responseEntityUtil.okBodyEntity(null, "01", "조회 결과가 없습니다.");
        }

        return responseEntityUtil.okBodyEntity(unescapedResultList);
    }

    @CommonApiResponses
    @PostMapping("/delete")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> deleteNotice(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {

        String gubun = (String) request.get("gubun");
        String noticeId = (String) request.get("noticeId");
        String title = (String) request.get("title");
        String content = (String) request.get("content");

        if (gubun == null || gubun.trim().isEmpty() || noticeId == null || noticeId.trim().isEmpty()) {
            return responseEntityUtil.okBodyEntity(null, "01", "파라미터가 잘못되어 있습니다.");
        }

        String rptCd = "NOTICETRAN";
        String jobGb = "SET";
        Claims claims = (Claims) httpRequest.getAttribute("user");
        String empNo = claims != null && claims.getSubject() != null ? claims.getSubject() : "admin";

        List<String> params = Arrays.asList(
                escapeUtil.escape(gubun),
                escapeUtil.escape(noticeId),
                escapeUtil.escape(empNo),
                escapeUtil.escape(title),
                escapeUtil.escape(content)
        );

        try {
            List<Map<String, Object>> resultList = mapViewProcessor.processDynamicView(rptCd, params, empNo, jobGb);

            if (resultList.isEmpty()) {
                return responseEntityUtil.okBodyEntity(null, "01", "게시물 저장 실패: 결과가 없습니다.");
            }

            Map<String, Object> result = resultList.get(0);
            Long getNoticeId = Long.parseLong(result.getOrDefault("NOTICEID", "-1").toString());
            if (getNoticeId == -1) {
                throw new IllegalArgumentException("NOTICEID 반환 실패");
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "게시물이 성공적으로 삭제되었습니다.");
            responseData.put("noticeId", getNoticeId);

            return responseEntityUtil.okBodyEntity(responseData);
        } catch (Exception e) {
            errorMessage = "/delete unescapedResultList = mapViewProcessor.processDynamicView(rptCd, params, empNo, jobGb);";
            logger.error(this.getErrorMessage(), e.getMessage(), e);
            return responseEntityUtil.okBodyEntity(null, "01", "게시물 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @CommonApiResponses
    @PostMapping("/save")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> saveNotice(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {

        String gubun = (String) request.get("gubun");
        String noticeId = (String) request.get("noticeId");
        String title = (String) request.get("title");
        String content = (String) request.get("content");

        if (gubun == null || gubun.trim().isEmpty() || noticeId == null || noticeId.trim().isEmpty()) {
            return responseEntityUtil.okBodyEntity(null, "01", "파라미터가 잘못되어 있습니다.");
        }

        if (title == null || title.trim().isEmpty() || content == null || content.trim().isEmpty()) {
            return responseEntityUtil.okBodyEntity(null, "01", "제목과 내용을 입력해주세요.");
        }

        String rptCd = "NOTICETRAN";
        String jobGb = "SET";
        Claims claims = (Claims) httpRequest.getAttribute("user");
        String empNo = claims != null && claims.getSubject() != null ? claims.getSubject() : "admin";

        List<String> params = Arrays.asList(
                escapeUtil.escape(gubun),
                escapeUtil.escape(noticeId),
                escapeUtil.escape(empNo),
                escapeUtil.escape(title),
                escapeUtil.escape(content)
        );

        try {
            List<Map<String, Object>> resultList = mapViewProcessor.processDynamicView(rptCd, params, empNo, jobGb);

            if (resultList.isEmpty()) {
                return responseEntityUtil.okBodyEntity(null, "01", "게시물 저장 실패: 결과가 없습니다.");
            }

            Map<String, Object> result = resultList.get(0);
            Long getNoticeId = Long.parseLong(result.getOrDefault("NOTICEID", "-1").toString());
            if (getNoticeId == -1) {
                throw new IllegalArgumentException("NOTICEID 반환 실패");
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("message", "게시물이 성공적으로 저장되었습니다.");
            responseData.put("noticeId", getNoticeId);

            return responseEntityUtil.okBodyEntity(responseData);
        } catch (Exception e) {
            errorMessage = "/save unescapedResultList = mapViewProcessor.processDynamicView(rptCd, params, empNo, jobGb);";
            logger.error(this.getErrorMessage(), e.getMessage(), e);
            return responseEntityUtil.okBodyEntity(null, "01", "게시물 저장 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @CommonApiResponses
    @PostMapping("/filelist")
    public ResponseEntity<ApiResponseDto<List<Map<String, Object>>>> noticeFileList(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest
    ) {
        String rptCd = "NOTICEFILE";
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
    public ResponseEntity<ApiResponseDto<List<MapViewFileEntity>>> saveFiles(
            String gubun,
            String fileId,
            String noticeId,
            MultipartFile[] files,
            HttpServletRequest httpRequest) {

        // Validate required parameters
        if (gubun == null || gubun.trim().isEmpty() || noticeId == null || noticeId.trim().isEmpty()) {
            return responseEntityUtil.okBodyEntity(null, "01", "gubun and noticeId are required.");
        }

        if (files == null || files.length == 0) {
            return responseEntityUtil.okBodyEntity(new ArrayList<>(), "00", "No files provided.");
        }

        if (files.length > fileConfig.getMaxFilesPerUpload()) {
            return responseEntityUtil.okBodyEntity(null, "01", "Too many files, maximum " + fileConfig.getMaxFilesPerUpload() + " allowed.");
        }

        String rptCd = "NOTICEFILETRAN";
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
                params.add(escapeUtil.escape(noticeId));
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
    public ResponseEntity<ApiResponseDto<List<MapViewFileEntity>>> deleteFiles(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {

        String gubun = (String) request.get("gubun");
        String fileId = (String) request.get("fileId");
        String noticeId = (String) request.get("noticeId");

        // Validate required parameters
        if (gubun == null || gubun.trim().isEmpty() || noticeId == null || noticeId.trim().isEmpty()) {
            return responseEntityUtil.okBodyEntity(null, "01", "gubun and noticeId are required.");
        }

        if (!"D".equals(gubun)) {
            return responseEntityUtil.okBodyEntity(null, "01", "Invalid gubun value for deletion. Must be 'D'.");
        }

        String rptCd = "NOTICEFILETRAN";
        String jobGb = "SET";

        Claims claims = (Claims) httpRequest.getAttribute("user");
        String empNo = claims != null && claims.getSubject() != null ? claims.getSubject() : "admin";

        List<MapViewFileEntity> result = new ArrayList<>();
        try {
            // Deletion does not require file data, only metadata
            List<Object> params = new ArrayList<>();
            params.add(escapeUtil.escape(gubun));
            params.add(escapeUtil.escape(fileId != null ? fileId : ""));
            params.add(escapeUtil.escape(noticeId));
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