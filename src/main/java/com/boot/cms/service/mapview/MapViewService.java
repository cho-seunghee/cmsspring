package com.boot.cms.service.mapview;

import com.boot.cms.entity.mapview.MapViewEntity;
import com.boot.cms.repository.mapview.MapViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MapViewService {
    private final MapViewRepository mapViewRepository;

    public boolean isValidParamCount(String rptCd, List<String> uiParams) {
        MapViewEntity procInfo = mapViewRepository.findMapViewInfoByRptCd(rptCd);
        if (procInfo == null) {
            throw new IllegalArgumentException("Invalid rptCd: " + rptCd);
        }
        int expectedParamCount = procInfo.getParamCnt();
        return uiParams.size() == expectedParamCount;
    }

    public String buildDynamicCall(String rptCd, List<String> uiParams) {
        MapViewEntity procInfo = mapViewRepository.findMapViewInfoByRptCd(rptCd);
        if (procInfo == null) {
            throw new IllegalArgumentException("Invalid rptCd: " + rptCd);
        }
        String procedureName = procInfo.getJobNm();
        String joinedParams = uiParams.stream()
                .map(p -> "'" + p.replace("'", "''") + "'")
                .collect(Collectors.joining(", "));
        return procedureName + "(" + joinedParams + ")";
    }

}
