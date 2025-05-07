package com.boot.cms.entity.sample;

import lombok.Data;

@Data
public class MybaitsClassMethodEntity {
    private String userid;
    private String usernm;

    @Override
    public String toString() {
        return "MybaitsClassMethodEntity{userid='" + userid + "', usernm='" + usernm + "'}";
    }
}
