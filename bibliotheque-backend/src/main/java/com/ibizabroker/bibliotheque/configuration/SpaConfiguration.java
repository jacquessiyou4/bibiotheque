package com.ibizabroker.bibliotheque.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Application fusionnée : le build Angular est embarqué dans le jar
 * (src/main/resources/static) et servi par Spring Boot lui-même, plus
 * besoin du conteneur nginx séparé.
 *
 * Le routage est géré côté client par l'Angular Router. Quand l'utilisateur
 * recharge (ou colle directement) une URL client comme /books ou /login,
 * aucun fichier ne correspond côté serveur : on retransmet alors index.html
 * (équivalent du `try_files $uri $uri/ /index.html;` de nginx.conf),
 * pour que le routeur Angular affiche le bon composant.
 */
@Configuration
public class SpaConfiguration implements WebMvcConfigurer {

    /** Les routes connues de l'Angular Router (voir app-routing.module.ts). */
    private static final String[] SPA_ROUTES = {
            "/books", "/create-book", "/update-book/*", "/book-details/*",
            "/users", "/register-user", "/user-details/*", "/update-user/*",
            "/login", "/logout", "/forbidden", "/borrow-book", "/return-book",
            "/reservations", "/borrow-list", "/home"
    };

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        for (String route : SPA_ROUTES) {
            registry.addViewController(route).setViewName("forward:/index.html");
        }
    }
}
