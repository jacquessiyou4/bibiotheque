package com.ibizabroker.bibliotheque.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfiguration {

    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String PUT = "PUT";
    private static final String PATCH = "PATCH";
    private static final String DELETE = "DELETE";

    // Le placeholder doit passer par @Value : écrit directement dans une
    // chaîne Java, il n'était jamais résolu et aucune origine n'était autorisée.
    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:4200}")
    private String allowedOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        final String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .toArray(String[]::new);
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        // PATCH : PATCH /api/reservations/{id}/annuler
                        .allowedMethods(GET, POST, PUT, PATCH, DELETE)
                        .allowedHeaders("*")
                        .allowedOriginPatterns(origins)
                        .allowCredentials(true);
            }
        };
    }
}
