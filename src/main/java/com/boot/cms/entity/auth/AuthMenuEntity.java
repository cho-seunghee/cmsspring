package com.boot.cms.entity.auth;

import lombok.Data;

@Data
public class AuthMenuEntity {
    private String menuId;
    private String menuNm;
    private String upperMenuId;
    private String menuLevel;
    private String menuOrder;
    private String url;
}
