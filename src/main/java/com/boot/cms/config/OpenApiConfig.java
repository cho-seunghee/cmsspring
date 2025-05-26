package com.boot.cms.config;

import com.boot.cms.util.JwtUtil;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    private final JwtUtil jwtUtil;

    @Value("${SWAGGER_UI_ENABLED:true}")
    private boolean isSwaggerUiEnabled;

    public OpenApiConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * Public API 그룹 구성
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public-api")
                .packagesToScan("com.boot.cms.controller")
                .build();
    }

    /**
     * OpenAPI 기본 설정 (Info, Security 등 포함)
     */
    @Bean
    public OpenAPI customOpenAPI() {

        String description = "현재 프로젝트 WEB을 Tab에서 로그인 후에 스웨거를 통해 테스트해야 권한오류가 나오지 않습니다.";

        return new OpenAPI()
                .info(new Info()
                        .title("CMS API")
                        .description(description) // 토큰 정보도 설명에 포함
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth")) // 기본 Security 요구사항
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
//                .extensions(extensions); // 확장 속성 추가
    }
}