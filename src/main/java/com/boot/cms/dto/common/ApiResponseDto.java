package com.boot.cms.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponseDto<T> {

    private boolean success;
    private T data;
    private String errCd;
    private String errMsg;

}
