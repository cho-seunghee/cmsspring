package com.boot.cms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration // Spring Security 설정 클래스
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                    .ignoringRequestMatchers("/api/public/**") // 공용 API 경로에서 CSRF 예외 처리
            )
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/public/**").permitAll() // 공용 경로에는 인증 불필요
                    .requestMatchers("/admin/**").hasRole("ADMIN") // 관리자 경로는 인증 요구
                    .anyRequest().authenticated() // 나머지도 인증 필요
            );


        return http.build();
    }
}
