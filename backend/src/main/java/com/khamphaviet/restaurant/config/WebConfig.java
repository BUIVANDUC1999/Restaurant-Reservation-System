package com.khamphaviet.restaurant.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final String[] allowedOrigins;
    private final String[] allowedOriginPatterns;

    public WebConfig(
            @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}") String[] allowedOrigins,
            @Value("${app.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*,http://192.168.*:*,http://10.*:*,http://172.*:*}") String[] allowedOriginPatterns) {
        this.allowedOrigins = allowedOrigins;
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    }
}
