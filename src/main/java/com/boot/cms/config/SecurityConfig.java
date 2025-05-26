package com.boot.cms.config;

import com.boot.cms.entity.auth.LoginEntity;
import com.boot.cms.mapper.auth.LoginMapper;
import com.boot.cms.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Collections;

@Configuration
public class SecurityConfig {

    private final Environment environment;
    private final LoginMapper loginMapper;

    public SecurityConfig(Environment environment, LoginMapper loginMapper) {
        this.environment = environment;
        this.loginMapper = loginMapper;
    }

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean isRender = isRenderEnvironment();
        if (isRender) {
            http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
        }

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정 적용
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))  // CSRF 예외 처리
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**")
                        .permitAll() // Swagger UI와 OpenAPI Docs 허용
                        .requestMatchers("/api/public/**", "/api/auth/**")
                        .permitAll() // 공개 API 허용
                        .requestMatchers("/api/**")
                        .authenticated() // /api/** 인증 필요
                        .anyRequest()
                        .authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class) // JWT 인증 필터
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {  // 인증 실패 처리
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"success\": false, \"message\": \"Unauthorized\"}");
                        })
                );

        return http.build();
    }

    private boolean isRenderEnvironment() {
        String env = environment.getProperty("PORT");
        return env != null && !env.isEmpty();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = getCorsConfiguration();
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/v3/api-docs/**", configuration); // CORS for Swagger
        source.registerCorsConfiguration("/swagger-ui/**", configuration); // CORS for Swagger UI
        return source;
    }

    @NotNull
    private CorsConfiguration getCorsConfiguration() {
        CorsConfiguration configuration = new CorsConfiguration();

        String[] originsArray = allowedOrigins.split(",");
        System.out.println("allowedOrigins: " + String.join(", ", originsArray));

        for (String origin : originsArray) {
            String trimmedOrigin = origin.trim();
            if (!trimmedOrigin.isEmpty()) {
                configuration.addAllowedOrigin(trimmedOrigin);
            }
        }

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setExposedHeaders(Arrays.asList("Authorization")); // Swagger에서 authorization 읽기 가능하도록
        configuration.setAllowCredentials(true); // JWT 같은 인증 데이터를 허용

        return configuration;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            LoginEntity user = loginMapper.loginCheck(username, null); // Password not needed for JWT
            if (user == null) {
                throw new UsernameNotFoundException("User not found: " + username);
            }
            return new User(
                    user.getEmpNo(),
                    user.getEmpPwd(),
                    Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority(user.getAuth()))
            );
        };
    }
}