package com.ibizabroker.bibliotheque.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private LoggingAccessDeniedHandler loggingAccessDeniedHandler;

    @Autowired
    private UserDetailsService jwtService;

    @Autowired
    @Qualifier("jwtAuthenticationConverter")
    private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.cors();
        httpSecurity.csrf().disable()
                .authorizeRequests().antMatchers("/authenticate", "/borrow/**", "/admin/books/").permitAll()
                .antMatchers(HttpHeaders.ALLOW).permitAll()
                .antMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // --- Application fusionnée : le build Angular est servi par
                // Spring Boot depuis /static --- On autorise sans jeton les
                // fichiers statiques du SPA et ses routes client (le routeur
                // Angular gère lui-même la garde AuthGuard). Les API REST
                // restent protégées par JWT : /admin/**, /me, /api/reservations...
                .antMatchers("/", "/index.html", "/favicon.ico").permitAll()
                .antMatchers("/assets/**").permitAll()
                .antMatchers("/*.js", "/*.css", "/*.map").permitAll()
                .antMatchers("/books", "/create-book", "/update-book/*", "/book-details/*",
                        "/users", "/register-user", "/user-details/*", "/update-user/*",
                        "/login", "/logout", "/forbidden", "/borrow-book", "/return-book",
                        "/reservations", "/borrow-list", "/home", "/error").permitAll()
                .anyRequest().authenticated()
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(loggingAccessDeniedHandler)
                .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                // Remplace le JWT maison (jjwt) : les jetons d'accès Keycloak sont
                // validés (resource server OAuth2) et leurs rôles traduits par
                // jwtAuthenticationConverter (ROLE_Admin / ROLE_User).
                .oauth2ResourceServer().authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .jwt().jwtAuthenticationConverter(jwtAuthenticationConverter);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
        authenticationManagerBuilder.userDetailsService(jwtService).passwordEncoder(passwordEncoder());
    }
}