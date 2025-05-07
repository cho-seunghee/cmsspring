package com.boot.cms.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String errCd;
    private String errMsg;

}
