package com.ibizabroker.bibliotheque.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springdoc.core.SwaggerUiConfigProperties;
import org.springdoc.core.SwaggerUiOAuthProperties;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StreamUtils;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Fait afficher l'API bibliothèque directement sur /swagger-ui/index.html.
 *
 * Le index.html de swagger-ui embarque l'URL de démo Petstore, et springdoc
 * 1.5.13 ne sait que la vider (« No API definition provided ») : seule
 * l'adresse /swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config
 * chargeait l'application. Une redirection de index.html vers lui-même
 * bouclerait ; on remplace donc, au moment de servir la page, l'URL de démo
 * par la configuration springdoc de l'application.
 *
 * springdoc déclare son propre transformer en @ConditionalOnMissingBean :
 * ce bean le remplace en conservant ses transformations (OAuth, CSRF...).
 */
@Configuration
public class SwaggerUiRedirectConfiguration {

    static final String CONFIG_URL = "/v3/api-docs/swagger-config";
    private static final String PETSTORE_URL = "url: \"https://petstore.swagger.io/v2/swagger.json\"";
    private static final String EMPTY_URL = "url: \"\"";
    private static final String INDEX_PATTERN = "**/swagger-ui/**/index.html";

    @Bean
    public SwaggerIndexTransformer indexPageTransformer(SwaggerUiConfigProperties swaggerUiConfig,
                                                       SwaggerUiOAuthProperties swaggerUiOAuthProperties,
                                                       ObjectMapper objectMapper) {
        return new SwaggerIndexPageTransformer(swaggerUiConfig, swaggerUiOAuthProperties, objectMapper) {
            private final AntPathMatcher antPathMatcher = new AntPathMatcher();

            @Override
            public Resource transform(HttpServletRequest request, Resource resource,
                                      ResourceTransformerChain transformerChain) throws IOException {
                Resource transformed = super.transform(request, resource, transformerChain);
                if (!antPathMatcher.match(INDEX_PATTERN, resource.getURL().toString())) {
                    return transformed;
                }
                String html = StreamUtils.copyToString(transformed.getInputStream(), StandardCharsets.UTF_8);
                String configUrl = "configUrl: \"" + CONFIG_URL + "\"";
                String patched = html.replace(PETSTORE_URL, configUrl).replace(EMPTY_URL, configUrl);
                return new TransformedResource(resource, patched.getBytes(StandardCharsets.UTF_8));
            }
        };
    }
}
