package com.boot.cms.model.license;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class LicenseInfo {
    private String library;
    private String license;
    private String url;
    private String copyright;
}