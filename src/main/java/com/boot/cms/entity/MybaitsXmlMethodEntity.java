package com.boot.cms.entity;

import lombok.Data;

@Data
public class MybaitsXmlMethodEntity {
    private String userid;
    private String usernm;

    @Override
    public String toString() {
        return "MybaitsClassMethodEntity{userid='" + userid + "', usernm='" + usernm + "'}";
    }
}
