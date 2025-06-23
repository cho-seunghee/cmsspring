package com.boot.cms.service.license;

import com.boot.cms.model.license.LicenseInfo;
import com.boot.cms.model.license.LicenseResponse;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Service
public class LicenseService {

    public List<LicenseInfo> getLicenseInfo() throws Exception {
        // ObjectMapper 생성 및 주석 허용 설정
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);

        // JSON 읽기
        InputStream inputStream = getClass().getResourceAsStream("/static/license/license.json");
        LicenseResponse response = mapper.readValue(inputStream, LicenseResponse.class);
        return response.getLicenses();
    }
}

