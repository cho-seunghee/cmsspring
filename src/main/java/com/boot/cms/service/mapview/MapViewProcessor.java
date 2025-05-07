package com.boot.cms.service.mapview;

import com.boot.cms.util.EscapeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MapViewProcessor {

    private final MapViewService mapViewService;
    private final DynamicQueryService dynamicQueryService;
    private final EscapeUtil escapeUtil;

    /**
     * 동적 프로시저 호출을 처리하고 결과를 반환합니다.
     *
     * @param rptCd  보고서 코드
     * @param params 파라미터 리스트
     * @return 처리된 결과 리스트 (unescaped), null이면 오류 발생
     * @throws IllegalArgumentException 파라미터 개수가 맞지 않을 경우
     */
    public List<Map<String, Object>> processDynamicView(String rptCd, List<String> params) {
        // Validate parameter count
        if (!mapViewService.isValidParamCount(rptCd, params)) {
            throw new IllegalArgumentException("입력한 인수 개수가 맞지 않습니다.");
        }

        // Build and execute dynamic procedure call
        String procedureCall = mapViewService.buildDynamicCall(rptCd, params);
        System.out.println("Dynamic Procedure Call: " + procedureCall); // Debugging
        List<Map<String, Object>> resultList = dynamicQueryService.executeDynamicQuery(procedureCall);

        // Unescape string values in the result
        List<Map<String, Object>> unescapedResultList = resultList.stream()
                .map(row -> {
                    Map<String, Object> unescapedRow = new LinkedHashMap<>();
                    row.forEach((key, value) -> {
                        if (value instanceof String) {
                            unescapedRow.put(key, EscapeUtil.unescape((String) value));
                        } else {
                            unescapedRow.put(key, value);
                        }
                    });
                    return unescapedRow;
                })
                .collect(Collectors.toList());

        System.out.println("Unescaped Result List: " + unescapedResultList); // Debugging

        return unescapedResultList;
    }
}