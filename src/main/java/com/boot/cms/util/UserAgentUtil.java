package com.boot.cms.util;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class UserAgentUtil {

    public String getUserAgent() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "unknown";
    }

    public String getUserCongb() {
        String userAgent = getUserAgent();
        if (userAgent.equals("unknown")) {
            return "W"; // Default to web if unknown
        }
        // Simple mobile detection based on common mobile keywords
        String userAgentLower = userAgent.toLowerCase();
        boolean isMobile = userAgentLower.contains("mobile") ||
                userAgentLower.contains("android") ||
                userAgentLower.contains("iphone") ||
                userAgentLower.contains("ipad");
        return isMobile ? "M" : "W";
    }
}