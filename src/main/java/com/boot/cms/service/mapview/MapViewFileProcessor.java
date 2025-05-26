package com.boot.cms.service.mapview;

import com.boot.cms.aspect.ClientIPAspect;
import com.boot.cms.config.AppConfig;
import com.boot.cms.entity.mapview.MapViewFileEntity;
import com.boot.cms.repository.mapview.MapViewFileRepository;
import com.boot.cms.util.EscapeUtil;
import com.boot.cms.util.UserAgentUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Base64;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MapViewFileProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MapViewFileProcessor.class);

    private final MapViewFileRepository mapViewFileRepository;
    private final DynamicQueryFileService dynamicQueryFileService;
    private final ClientIPAspect clientIPAspect;
    private final UserAgentUtil userAgentUtil;
    private final EscapeUtil escapeUtil;
    private final AppConfig.FileConfig fileConfig;

    @Setter
    @Getter
    String errorMessage;

    public MapViewFileEntity validateAndBuildFileCall(String rptCd, List<Object> params, String empNo, String jobGb) {
        String ip = clientIPAspect.getClientIP();
        String userAgent = userAgentUtil.getUserAgent();
        String userCongb = userAgentUtil.getUserCongb();

        // Replace longblob-related parameters (byte[]) with "DATA" for UP_MAPVIEWFILES_SELECT
        String pParams = params.stream()
                .map(param -> param instanceof byte[] ? "DATA" : param.toString())
                .collect(Collectors.joining("│"));

        MapViewFileEntity procInfo = mapViewFileRepository.findFileInfoByCriteria(empNo, ip, rptCd, jobGb, pParams, userCongb, userAgent);
        if (procInfo == null) {
            logger.error("No file information found for rptCd: {}", rptCd);
            throw new IllegalArgumentException("No file information found for rptCd: " + rptCd);
        }

        if (!"00".equals(procInfo.getErrCd())) {
            logger.error("File lookup failed: ErrCd={}, ErrMsg={}", procInfo.getErrCd(), procInfo.getErrMsg());
            throw new IllegalArgumentException("File lookup failed: " + procInfo.getErrMsg());
        }

        int expectedParamCount = procInfo.getParamCnt();
        if (params.size() != expectedParamCount) {
            logger.error("Invalid parameter count for rptCd: {}, expected: {}, provided: {}", rptCd, expectedParamCount, params.size());
            throw new IllegalArgumentException("파라미터 개수가 일치하지 않습니다.");
        }

        String procedureName = procInfo.getJobNm();
        // Store original params for dynamic call
        String joinedParams = params.stream()
                .map(param -> {
                    if (param instanceof byte[]) {
                        byte[] data = (byte[]) param;
                        if (data.length > fileConfig.getMaxFileSize()) {
                            throw new IllegalArgumentException("File size exceeds " + (fileConfig.getMaxFileSize() / (1024 * 1024)) + "MB limit");
                        }
                        return "[" + Base64.getEncoder().encodeToString(data) + "]";
                    } else {
                        return "'" + param.toString().replace("'", "''") + "'";
                    }
                })
                .collect(Collectors.joining(", "));

        procInfo.setDynamicCall(procedureName + "(" + joinedParams + ")");
        return procInfo;
    }

    public List<MapViewFileEntity> processFileUpload(String rptCd, List<Object> params, String empNo, String jobGb) {
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("Parameters are required.");
        }

        MapViewFileEntity procInfo = validateAndBuildFileCall(rptCd, params, empNo, jobGb);
        int expectedParamCount = procInfo.getParamCnt();
        if (params.size() != expectedParamCount) {
            throw new IllegalArgumentException("파라미터 개수가 일치하지 않습니다.");
        }

        List<MapViewFileEntity> result = new ArrayList<>();
        try {
            List<Map<String, Object>> rawResult = dynamicQueryFileService.executeDynamicFileQuery(procInfo.getDynamicCall(), params);
            if (rawResult.size() > fileConfig.getMaxResultSize()) {
                logger.warn("Result size exceeds limit: {}", rawResult.size());
                rawResult = rawResult.subList(0, fileConfig.getMaxResultSize());
            }
            for (Map<String, Object> row : rawResult) {
                MapViewFileEntity entity = new MapViewFileEntity();
                entity.setErrCd((String) row.getOrDefault("ERRCD", "00"));
                entity.setErrMsg((String) row.getOrDefault("ERRMSG", ""));
                result.add(entity);
            }
        } catch (IllegalArgumentException e) {
            errorMessage = "processFileUpload: executeDynamicFileQuery failed for rptCd: " + rptCd;
            logger.error(this.getErrorMessage(), e.getMessage(), e);
            throw e;
        }

        return result;
    }

    public List<MapViewFileEntity> processFileDelete(String rptCd, List<Object> params, String empNo, String jobGb) {
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("Parameters are required.");
        }

        MapViewFileEntity procInfo = validateAndBuildFileCall(rptCd, params, empNo, jobGb);
        int expectedParamCount = procInfo.getParamCnt();
        if (params.size() != expectedParamCount) {
            throw new IllegalArgumentException("파라미터 개수가 일치하지 않습니다.");
        }

        List<MapViewFileEntity> result = new ArrayList<>();
        try {
            List<Map<String, Object>> rawResult = dynamicQueryFileService.executeDynamicFileQuery(procInfo.getDynamicCall(), params);
            if (rawResult.size() > fileConfig.getMaxResultSize()) {
                logger.warn("Result size exceeds limit: {}", rawResult.size());
                rawResult = rawResult.subList(0, fileConfig.getMaxResultSize());
            }
            for (Map<String, Object> row : rawResult) {
                MapViewFileEntity entity = new MapViewFileEntity();
                entity.setErrCd((String) row.getOrDefault("ERRCD", "00"));
                entity.setErrMsg((String) row.getOrDefault("ERRMSG", ""));
                result.add(entity);
            }
        } catch (IllegalArgumentException e) {
            errorMessage = "processFileDelete: executeDynamicFileQuery failed for rptCd: " + rptCd;
            logger.error(this.getErrorMessage(), e.getMessage(), e);
            throw e;
        }

        return result;
    }

    public List<MapViewFileEntity> processFileRetrieval(String rptCd, List<Object> params, String empNo, String jobGb) {
        MapViewFileEntity procInfo = validateAndBuildFileCall(rptCd, params, empNo, jobGb);
        List<MapViewFileEntity> result = new ArrayList<>();

        try {
            List<Map<String, Object>> rawResult = dynamicQueryFileService.executeDynamicFileQuery(procInfo.getDynamicCall(), params);
            if (rawResult.size() > fileConfig.getMaxResultSize()) {
                logger.warn("Result size exceeds limit: {}", rawResult.size());
                rawResult = rawResult.subList(0, fileConfig.getMaxResultSize());
            }
            for (Map<String, Object> row : rawResult) {
                MapViewFileEntity entity = new MapViewFileEntity();
                entity.setErrCd((String) row.getOrDefault("ERRCD", "00"));
                entity.setErrMsg((String) row.getOrDefault("ERRMSG", ""));
                result.add(entity);
            }
        } catch (IllegalArgumentException e) {
            errorMessage = "processFileRetrieval: executeDynamicFileQuery failed for rptCd: " + rptCd;
            logger.error(this.getErrorMessage(), e.getMessage(), e);
            throw e;
        }

        return result;
    }

    public List<Map<String, Object>> processDynamicView(String rptCd, List<String> params, String empNo, String jobGb) {
        if (empNo == null || empNo.trim().isEmpty()) {
            logger.error("Authentication error: empNo is null or empty");
            throw new IllegalArgumentException("인증 정보가 없습니다.");
        }

        String ip = clientIPAspect.getClientIP();
        String userAgent = userAgentUtil.getUserAgent();
        String userCongb = userAgentUtil.getUserCongb();

        MapViewFileEntity procInfo;
        try {
            String pParams = params.stream().collect(Collectors.joining("│"));
            procInfo = mapViewFileRepository.findFileInfoByCriteria(empNo, ip, rptCd, jobGb, pParams, userCongb, userAgent);
            if (procInfo == null) {
                logger.error("No file information found for rptCd: {}", rptCd);
                throw new IllegalArgumentException("No file information found for rptCd: " + rptCd);
            }

            if (!"00".equals(procInfo.getErrCd())) {
                logger.error("File lookup failed: ErrCd={}, ErrMsg={}", procInfo.getErrCd(), procInfo.getErrMsg());
                throw new IllegalArgumentException("File lookup failed: " + procInfo.getErrMsg());
            }

            int expectedParamCount = procInfo.getParamCnt();
            if (params.size() != expectedParamCount) {
                logger.error("Invalid parameter count for rptCd: {}, expected: {}, provided: {}", rptCd, expectedParamCount, params.size());
                throw new IllegalArgumentException("파라미터 개수가 일치하지 않습니다.");
            }

            String procedureName = procInfo.getJobNm();
            String joinedParams = params.stream()
                    .map(param -> "'" + param.replace("'", "''") + "'")
                    .collect(Collectors.joining(", "));
            procInfo.setDynamicCall(procedureName + "(" + joinedParams + ")");
        } catch (IllegalArgumentException e) {
            errorMessage = "Validation error for rptCd: " + rptCd;
            logger.error(this.getErrorMessage(), e.getMessage(), e);
            throw e;
        }

        List<Map<String, Object>> resultList;
        try {
            resultList = dynamicQueryFileService.executeDynamicQuery(procInfo.getDynamicCall());
        } catch (IllegalArgumentException e) {
            errorMessage = "Database error for rptCd: " + rptCd;
            logger.error(this.getErrorMessage(), e.getMessage(), e);
            throw new IllegalArgumentException(this.getErrorMessage() + ": " + e.getMessage());
        }

        List<Map<String, Object>> unescapedResultList = resultList.stream()
                .map(row -> {
                    Map<String, Object> unescapedRow = new LinkedHashMap<>();
                    row.forEach((key, value) -> {
                        if (value instanceof String) {
                            String unescaped = escapeUtil.unescape((String) value);
                            unescapedRow.put(key, unescaped);
                        } else {
                            unescapedRow.put(key, value);
                        }
                    });
                    return unescapedRow;
                })
                .collect(Collectors.toList());

        return unescapedResultList;
    }
}