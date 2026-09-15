package com.ibizabroker.bibliotheque.shared.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Corps d'erreur unique de l'API, au format RFC 7807
 * (application/problem+json) : le même pour une erreur métier, un 401 du
 * filtre de sécurité ou un 500.
 *
 * <pre>
 * { "type": ".../problemes/book-unavailable", "title": "Bad Request", "status": 400,
 *   "detail": "Le livre ... n'est plus disponible.", "code": "BOOK_UNAVAILABLE",
 *   "message": "(alias de detail)", "instance": "/borrow", "requestId": "3f2a...",
 *   "timestamp": "2026-09-15T10:15:30Z" }
 * </pre>
 */
public final class ProblemeHttp {

    public static final MediaType MEDIA_TYPE = MediaType.valueOf("application/problem+json");

    /** Clé MDC du X-Request-ID, posée par SecurityAuditFilter. */
    public static final String MDC_REQUEST_ID = "requestId";

    static final String TYPE_BASE = "https://bibliotheque.local/problemes/";

    private ProblemeHttp() {
    }

    public static Map<String, Object> corps(HttpStatus status, String code, String detail) {
        return corps(status, code, detail, null);
    }

    public static Map<String, Object> corps(HttpStatus status, String code, String detail, String instance) {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("type", TYPE_BASE + code.toLowerCase(Locale.ROOT).replace('_', '-'));
        corps.put("title", status.getReasonPhrase());
        corps.put("status", status.value());
        corps.put("detail", detail);
        corps.put("code", code);
        // Alias de « detail », conservé pour les clients écrits avant ce format.
        corps.put("message", detail);
        if (instance != null) {
            corps.put("instance", instance);
        }
        String requestId = MDC.get(MDC_REQUEST_ID);
        if (requestId != null) {
            corps.put("requestId", requestId);
        }
        corps.put("timestamp", Instant.now().toString());
        return corps;
    }

    /** Pour les composants hors Spring MVC (filtres de sécurité). */
    public static void ecrire(HttpServletResponse response, ObjectMapper objectMapper, HttpStatus status,
                              String code, String detail, String instance) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MEDIA_TYPE + ";charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(corps(status, code, detail, instance)));
    }
}
