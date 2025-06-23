package com.boot.cms.model.license;

import lombok.Data;
import java.util.List;

@Data
public class LicenseResponse {
    private List<LicenseInfo> licenses;
}

