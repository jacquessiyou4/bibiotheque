package com.ibizabroker.bibliotheque.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SwaggerUiRedirectConfiguration implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Pas de redirect : /swagger-ui/index.html affiche Petstore par défaut,
        // /swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config affiche l'API de l'app.
    }
}
