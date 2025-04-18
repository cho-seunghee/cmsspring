package com.boot.cms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    // application.properties로부터 설정값 읽기
    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    @Value("${cors.allowed-methods}")
    private String[] allowedMethods;

    @Value("${cors.allowed-headers}")
    private String[] allowedHeaders;

    @Value("${cors.allow-credentials}")
    private boolean allowCredentials;

    @Value("${cors.max-age}")
    private long maxAge;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // 모든 경로에 대해 CORS 설정 적용
                        .allowedOrigins(allowedOrigins) // 허용할 Origin 목록
                        .allowedMethods(allowedMethods) // 허용할 HTTP 메서드
                        .allowedHeaders(allowedHeaders) // 허용할 헤더
                        .allowCredentials(allowCredentials) // 인증 정보 허용 여부
                        .maxAge(maxAge); // Preflight 요청 캐싱 시간 (초)
            }
        };
    }
}