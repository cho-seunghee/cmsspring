package com.boot.cms.service.mapview;

import com.boot.cms.entity.mapview.MapViewEntity;
import com.boot.cms.repository.mapview.MapViewRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MapViewService {

    private static final Logger logger = LoggerFactory.getLogger(MapViewService.class);

    private final MapViewRepository mapViewRepository;

    public MapViewEntity validateAndBuildCall(String rptCd, List<String> uiParams, String empNo, String ip, String jobGb, String userCongb, String userAgent) {
        String pParams = "";
        if (uiParams.size() >= 2) {
            pParams = uiParams.stream()
                    .collect(Collectors.joining("│"));
        }
        else {
            pParams = uiParams.get(0);
        }

        MapViewEntity procInfo = mapViewRepository.findMapViewInfoByRptCd(empNo, ip, rptCd, jobGb, pParams, userCongb, userAgent);
        if (procInfo == null) {
            logger.error("No procedure information found for rptCd: {}", rptCd);
            throw new IllegalArgumentException("No procedure information found for rptCd: " + rptCd);
        }

        if (!"00".equals(procInfo.getErrCd())) {
            logger.error("Procedure lookup failed: ErrCd={}, ErrMsg={}", procInfo.getErrCd(), procInfo.getErrMsg());
            throw new IllegalArgumentException("Procedure lookup failed: " + procInfo.getErrMsg());
        }

        int expectedParamCount = procInfo.getParamCnt();
        if (uiParams.size() != expectedParamCount) {
            logger.error("Invalid parameter count for rptCd: {}, expected: {}, provided: {}", rptCd, expectedParamCount, uiParams.size());
            throw new IllegalArgumentException("파라미터 개수가 일치하지 않습니다.");
        }

        String procedureName = procInfo.getJobNm();
        String joinedParams = uiParams.stream()
                .map(p -> "'" + p.replace("'", "''") + "'")
                .collect(Collectors.joining(", "));

        procInfo.setDynamicCall(procedureName + "(" + joinedParams + ")");
        return procInfo;
    }
}