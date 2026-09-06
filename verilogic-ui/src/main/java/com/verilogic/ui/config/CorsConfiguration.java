package com.verilogic.ui.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Restricts browser origins instead of a wildcard CORS policy.
 */
@Configuration
public class CorsConfiguration implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfiguration(
            @Value("${verilogic.cors.allowed-origins:http://localhost:8080}") String allowedOrigins
    ) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("Content-Type", "X-Requested-With", "X-Forwarded-For")
                .exposedHeaders(
                        "X-VeriLogic-Decision",
                        "X-VeriLogic-Certificate-Id",
                        "X-VeriLogic-Case-Id",
                        "X-VeriLogic-Merkle-Root",
                        "X-VeriLogic-Execution-Time-Ms",
                        "X-VeriLogic-Trace-Id",
                        "X-RateLimit-Limit",
                        "X-RateLimit-Remaining",
                        "Retry-After"
                )
                .allowCredentials(false)
                .maxAge(3600);
    }
}
