package com.ibizabroker.bibliotheque.shared.web;

import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Anciennes URL de l'API (sans version) : toujours servies, mais annoncées
 * comme obsolètes.
 *
 * L'API est publiée sous /api/v1 (contrôleurs et Swagger). Une requête sur un
 * ancien chemin, par exemple /borrow/user/3, est traitée comme
 * /api/v1/loans/user/3 et reçoit :
 * <ul>
 *   <li>{@code Deprecation: true} ;</li>
 *   <li>{@code Sunset} : date à partir de laquelle l'ancien chemin sera supprimé ;</li>
 *   <li>{@code Link: </api/v1/loans/user/3>; rel="successor-version"}.</li>
 * </ul>
 * Placé avant Spring Security : les règles d'accès ne connaissent que les
 * chemins versionnés, un ancien chemin ne peut pas les contourner.
 */
public class ApiVersionFilter extends OncePerRequestFilter {

    /** Suppression prévue des anciens chemins (six mois après la v1). */
    static final String SUNSET = "Mon, 15 Mar 2027 00:00:00 GMT";

    /** Ancien préfixe -> préfixe versionné, du plus spécifique au plus général. */
    static final Map<String, String> ANCIENS_PREFIXES = new LinkedHashMap<>();

    static {
        ANCIENS_PREFIXES.put("/api/reservations", "/api/v1/reservations");
        ANCIENS_PREFIXES.put("/admin/books", "/api/v1/books");
        ANCIENS_PREFIXES.put("/admin/users", "/api/v1/users");
        ANCIENS_PREFIXES.put("/borrow", "/api/v1/loans");
        ANCIENS_PREFIXES.put("/auth", "/api/v1/auth");
        ANCIENS_PREFIXES.put("/profile", "/api/v1/profile");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String chemin = request.getRequestURI().substring(request.getContextPath().length());
        String versionne = versionner(chemin);
        if (versionne == null) {
            chain.doFilter(request, response);
            return;
        }
        response.setHeader("Deprecation", "true");
        response.setHeader("Sunset", SUNSET);
        response.setHeader(HttpHeaders.LINK, "<" + request.getContextPath() + versionne + ">; rel=\"successor-version\"");
        chain.doFilter(new RequeteVersionnee(request, versionne), response);
    }

    /** @return le chemin versionné correspondant, ou null si le chemin n'est pas un ancien chemin */
    static String versionner(String chemin) {
        for (Map.Entry<String, String> entree : ANCIENS_PREFIXES.entrySet()) {
            String ancien = entree.getKey();
            if (chemin.equals(ancien) || chemin.startsWith(ancien + "/")) {
                return entree.getValue() + chemin.substring(ancien.length());
            }
        }
        return null;
    }

    /** Requête dont le chemin est remplacé par son équivalent versionné. */
    static final class RequeteVersionnee extends HttpServletRequestWrapper {

        private final String chemin;

        RequeteVersionnee(HttpServletRequest request, String chemin) {
            super(request);
            this.chemin = chemin;
        }

        @Override
        public String getRequestURI() {
            return getContextPath() + chemin;
        }

        @Override
        public StringBuffer getRequestURL() {
            StringBuffer url = new StringBuffer();
            url.append(getScheme()).append("://").append(getServerName());
            int port = getServerPort();
            if (port > 0 && !(("http".equals(getScheme()) && port == 80) || ("https".equals(getScheme()) && port == 443))) {
                url.append(':').append(port);
            }
            return url.append(getRequestURI());
        }

        // Même découpage servletPath / pathInfo que la requête d'origine : le
        // DispatcherServlet (mappé sur « / ») et les règles de Spring Security
        // retrouvent ainsi le chemin versionné.
        @Override
        public String getServletPath() {
            String servletPath = super.getServletPath();
            return servletPath == null || servletPath.isEmpty() ? servletPath : chemin;
        }

        @Override
        public String getPathInfo() {
            String servletPath = super.getServletPath();
            return servletPath == null || servletPath.isEmpty() ? chemin : null;
        }
    }
}
