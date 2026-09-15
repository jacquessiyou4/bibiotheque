package com.ibizabroker.bibliotheque.utilisateurs.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Déclare le filtre de liaison des comptes Keycloak. La configuration de
 * sécurité (shared) le récupère par son nom, sans dépendre des classes
 * internes de la fonctionnalité utilisateurs.
 */
@Configuration
public class CompteKeycloakConfiguration {

    @Bean
    public CompteKeycloakFilter compteKeycloakFilter(UsersRepository usersRepository, ObjectMapper objectMapper) {
        return new CompteKeycloakFilter(usersRepository, objectMapper);
    }

    /**
     * Le filtre doit s'exécuter après la validation du jeton, dans la chaîne
     * Spring Security : Spring Boot ne l'ajoute pas en plus à la chaîne des servlets.
     */
    @Bean
    public FilterRegistrationBean<CompteKeycloakFilter> compteKeycloakFilterHorsServlet(CompteKeycloakFilter filtre) {
        FilterRegistrationBean<CompteKeycloakFilter> enregistrement = new FilterRegistrationBean<>(filtre);
        enregistrement.setEnabled(false);
        return enregistrement;
    }
}
