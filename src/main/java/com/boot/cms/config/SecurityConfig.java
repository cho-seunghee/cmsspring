package com.boot.cms.config;

import com.boot.cms.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.core.env.Environment;

@Configuration
public class SecurityConfig {

    private final Environment environment;

    // Environment 객체를 통해 로컬/프로덕션(Runtime 환경) 분기 처리
    public SecurityConfig(Environment environment) {
        this.environment = environment;
    }

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Render와 로컬 환경을 구분하여 HTTPS 리디렉션 처리
        boolean isRender = isRenderEnvironment();
        if (isRender) {
            http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
        }

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/public/**")
                        .ignoringRequestMatchers("/api/auth/**")
                        .ignoringRequestMatchers("/api/mapview/**")
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/mapview/**").authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"success\": false, \"message\": \"Unauthorized\"}");
                        })
                );

        return http.build();
    }

    private boolean isRenderEnvironment() {
        String env = environment.getProperty("PORT"); // Render는 PORT 환경 변수 설정
        return env != null && !env.isEmpty();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin(allowedOrigins);
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/auth/**", configuration);
        source.registerCorsConfiguration("/api/mapview/**", configuration);
        source.registerCorsConfiguration("/api/public/**", configuration);
        return source;
    }
}