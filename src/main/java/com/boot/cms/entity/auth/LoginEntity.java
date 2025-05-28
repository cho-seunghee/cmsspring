package com.boot.cms.entity.auth;

import lombok.Data;

@Data
public class LoginEntity {
    private String empNo;
    private String empNm;
    private String empPwd;
    private String auth;
    private String orgCd;
    private String pwdChgYn;
    private String clientIP;
}
