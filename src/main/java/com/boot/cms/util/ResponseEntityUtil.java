package com.boot.cms.util;

import com.boot.cms.dto.common.ApiResponseDto;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class ResponseEntityUtil {
    private static final Logger logger = LoggerFactory.getLogger(ResponseEntityUtil.class);

    // 디폴트 에러 메시지 상수
    private static final String DEFAULT_PARAM_ERROR_MESSAGE = "파라미터가 잘못되었습니다.";
    private static final String DEFAULT_EMPTY_RESULT_MESSAGE = "조회 결과가 없습니다.";

    @Setter
    @Getter
    String errorMessage;

    //정상적으로 데이터만 출력
    public <T> ResponseEntity<ApiResponseDto<T>> okBodyEntity(T data) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, data, "00", ""));
    }

    //정상적이지만 에러코드와 에러메시지를 출력
    public <T> ResponseEntity<ApiResponseDto<T>> okBodyEntity(T data, String cd, String msg) {
        return ResponseEntity.ok(new ApiResponseDto<>(true, data, cd, msg));
    }

    //네트워크 오류 등 비정상적일때 출력
    public <T> ResponseEntity<ApiResponseDto<T>> errBodyEntity(String msg) {
        return ResponseEntity.badRequest()
                .body(new ApiResponseDto<>(false, null, "01", msg));
    }

    //네트워크 오류 등 비정상적일때 출력
    public <T> ResponseEntity<ApiResponseDto<T>> errBodyEntity(String msg, int statusCode) {
        return ResponseEntity.status(statusCode)
                .body(new ApiResponseDto<>(false, null, "01", msg));
    }

    // 여러 파라미터 키를 검증하고 리스트 조회를 처리하는 공통 메서드
    public <T> ResponseEntity<ApiResponseDto<List<T>>> handleListQuery(
            Map<String, String> request,
            List<String> paramKeys,
            Function<Map<String, String>, List<T>> queryFunction,
            String paramErrorMessage,
            String emptyResultMessage
    ) {

        if(paramErrorMessage.isEmpty()) paramErrorMessage = DEFAULT_PARAM_ERROR_MESSAGE;
        if(emptyResultMessage.isEmpty()) emptyResultMessage = DEFAULT_EMPTY_RESULT_MESSAGE;

        // 파라미터 검증
        for (String paramKey : paramKeys) {
            String paramValue = request.get(paramKey);
            if (paramValue == null || paramValue.isEmpty()) {
                return okBodyEntity(null, "01", paramErrorMessage + " (" + paramKey + ")");
            }
        }

        // 데이터 조회
        List<T> resultList;
        try {
            resultList = queryFunction.apply(request);
        } catch (Exception e) {
            errorMessage = "조회 중 오류 발생: ";
            logger.error(this.getErrorMessage(), e.getMessage(), e);
            return errBodyEntity(this.getErrorMessage() + e.getMessage());
        }

        // 조회 결과 없음 처리
        if (resultList == null || resultList.isEmpty()) {
            return okBodyEntity(null, "01", emptyResultMessage);
        }

        // 성공 응답
        return okBodyEntity(resultList);
    }

}
