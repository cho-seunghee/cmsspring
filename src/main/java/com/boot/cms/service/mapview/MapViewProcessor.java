// File: com/boot/cms/service/mapview/MapViewProcessor.java
package com.boot.cms.service.mapview;

import com.boot.cms.aspect.ClientIPAspect;
import com.boot.cms.entity.mapview.MapViewEntity;
import com.boot.cms.util.EscapeUtil;
import com.boot.cms.util.UserAgentUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MapViewProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MapViewProcessor.class);

    private final MapViewService mapViewService;
    private final DynamicQueryService dynamicQueryService;
    private final EscapeUtil escapeUtil;
    private final ClientIPAspect clientIPAspect;
    private final UserAgentUtil userAgentUtil;

    @Setter
    @Getter
    String errorMessage;

    public List<Map<String, Object>> processDynamicView(String rptCd, List<String> params, String empNo, String jobGb) {
        // 1. 인증 정보 검증
        if (empNo == null || empNo.trim().isEmpty()) {
            logger.error("Authentication error: empNo is null or empty");
            throw new IllegalArgumentException("인증 정보가 없습니다.");
        }

        // Get IP, UserAgent, and UserCongb
        String ip = clientIPAspect.getClientIP();
        String userAgent = userAgentUtil.getUserAgent();
        String userCongb = userAgentUtil.getUserCongb();

        // 2. Validate and build dynamic call for the procedure
        MapViewEntity procInfo;
        try {
            procInfo = mapViewService.validateAndBuildCall(rptCd, params, empNo, ip, jobGb, userCongb, userAgent);
        } catch (IllegalArgumentException e) {
            errorMessage = "Validation error for rptCd: {}, error: {}";
            logger.error(this.getErrorMessage(), rptCd, e.getMessage());
            throw e;
        }

        String procedureCall = procInfo.getDynamicCall();

        // 3. Execute the dynamic query
        List<Map<String, Object>> resultList;
        try {
            resultList = dynamicQueryService.executeDynamicQuery(procedureCall);
        } catch (IllegalArgumentException e) {
            errorMessage = "데이터베이스 오류: ";
            logger.error(this.getErrorMessage(), procedureCall, e.getMessage(), e);
            throw new IllegalArgumentException(this.getErrorMessage() + e.getMessage());
        }

        // 4. Unescape and restore string values in the result
        List<Map<String, Object>> unescapedResultList = resultList.stream()
                .map(row -> {
                    Map<String, Object> unescapedRow = new LinkedHashMap<>();
                    row.forEach((key, value) -> {
                        if (value instanceof String) {
                            // First unescape, then restore
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