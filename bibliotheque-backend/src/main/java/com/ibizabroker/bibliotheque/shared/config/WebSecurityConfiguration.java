package com.ibizabroker.bibliotheque.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibizabroker.bibliotheque.shared.web.JwtAuthenticationEntryPoint;
import com.ibizabroker.bibliotheque.shared.web.LimiteConnexionFilter;
import com.ibizabroker.bibliotheque.shared.web.LoggingAccessDeniedHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;

import javax.servlet.Filter;
import java.time.Clock;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final LoggingAccessDeniedHandler loggingAccessDeniedHandler;
    private final Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;
    private final Filter compteKeycloakFilter;
    private final ObjectMapper objectMapper;

    /**
     * compteKeycloakFilter est fourni par la fonctionnalité utilisateurs
     * (CompteKeycloakConfiguration) : reçu comme simple Filter, shared ne
     * dépend d'aucune classe de fonctionnalité.
     */
    public WebSecurityConfiguration(JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                                    LoggingAccessDeniedHandler loggingAccessDeniedHandler,
                                    @Qualifier("jwtAuthenticationConverter")
                                    Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter,
                                    @Qualifier("compteKeycloakFilter") Filter compteKeycloakFilter,
                                    ObjectMapper objectMapper) {
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.loggingAccessDeniedHandler = loggingAccessDeniedHandler;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.compteKeycloakFilter = compteKeycloakFilter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.cors();
        httpSecurity.csrf().disable()
                .authorizeRequests()
                // Connexion Keycloak : on n'a pas encore de jeton valide.
                .antMatchers(HttpMethod.POST, "/api/v1/auth/token").permitAll()
                .antMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // Healthcheck Docker (sans jeton) ; les autres endpoints Actuator restent protégés.
                .antMatchers("/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                // Métriques : Prometheus (HTTP Basic, voir AccesMetriques) ou un administrateur.
                .antMatchers("/actuator/prometheus").access("@accesMetriques.autorise(request) or hasRole('Admin')")
                // Page d'erreur Spring (réponses 404/405 hors API). Le frontend Angular
                // est servi par nginx : le backend n'expose plus que l'API.
                .antMatchers("/error").permitAll()
                .anyRequest().authenticated()
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(loggingAccessDeniedHandler)
                .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                // Seuls les jetons d'accès Keycloak sont acceptés (resource server
                // OAuth2) ; leurs rôles sont traduits par jwtAuthenticationConverter
                // (ROLE_Admin / ROLE_User).
                .oauth2ResourceServer().authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .jwt().jwtAuthenticationConverter(jwtAuthenticationConverter);

        // Une fois le jeton validé : relie le compte local au compte Keycloak (claim sub).
        httpSecurity.addFilterAfter(compteKeycloakFilter, BearerTokenAuthenticationFilter.class);
        // Freine les essais de mots de passe en série sur POST /api/v1/auth/token (429).
        httpSecurity.addFilterBefore(new LimiteConnexionFilter(objectMapper, Clock.systemUTC()),
                BearerTokenAuthenticationFilter.class);
    }

    // Toujours utilisé : AdminController hache le mot de passe des comptes locaux.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
