package com.ibizabroker.bibliotheque.shared.config;

import com.ibizabroker.bibliotheque.shared.web.ApiVersionFilter;
import com.ibizabroker.bibliotheque.shared.web.SecurityAuditFilter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class ApiVersionConfiguration {

    /**
     * Juste après SecurityAuditFilter (qui journalise le chemin demandé tel quel)
     * et avant Spring Security (qui ne doit voir que les chemins versionnés).
     */
    @Bean
    public FilterRegistrationBean<ApiVersionFilter> apiVersionFilter() {
        FilterRegistrationBean<ApiVersionFilter> enregistrement = new FilterRegistrationBean<>(new ApiVersionFilter());
        enregistrement.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        enregistrement.addUrlPatterns("/*");
        return enregistrement;
    }
}
