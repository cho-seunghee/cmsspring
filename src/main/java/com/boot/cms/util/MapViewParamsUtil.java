package com.boot.cms.util;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MapViewParamsUtil {

    public List<String> getParams(Map<String, Object> request, EscapeUtil escapeUtil) {
        return request.entrySet().stream()
                .filter(entry -> !List.of("rptCd", "jobGb", "empNo").contains(entry.getKey()))
                .map(entry -> escapeUtil.escape(String.valueOf(entry.getValue())))
                .collect(Collectors.toList());
    }
}