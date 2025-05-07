package com.boot.cms.entity.oper;

import lombok.Data;

@Data
public class MenuAuthEntity {
    private String menuId;
    private String menuNm;
    private int menuLevel;
    private String upperMenuId;
    private int menuOrder;
    private String authId;
    private String authNm;
    private String authYn;
}
