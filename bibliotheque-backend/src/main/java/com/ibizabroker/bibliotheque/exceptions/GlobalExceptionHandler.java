package com.ibizabroker.bibliotheque.exceptions;

import com.ibizabroker.bibliotheque.configuration.SecurityAuditFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(BadRequestException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        journaliserRefus(request, ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, String>> handleConflict(ConflictException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        // getFieldErrors() plutôt qu'un cast de getAllErrors() : une erreur
        // globale (niveau classe) provoquait une ClassCastException.
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage()));
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Erreur de validation");
        body.put("errors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(ConstraintViolationException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // JSON mal formé ou paramètre au mauvais type (ex. ?statut=INCONNU) :
    // erreurs du client, pas des 500.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleNotReadable(HttpMessageNotReadableException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Corps de requête invalide");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Valeur invalide pour le paramètre « " + ex.getName() + " »");
    }

    // Tri sur une propriété inconnue (ex. ?sortBy=inexistant) : Spring Data
    // lève PropertyReferenceException, parfois enveloppée par la traduction
    // d'exceptions de persistance.
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<Map<String, String>> handlePropertyReference(PropertyReferenceException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Propriété de tri inconnue : « " + ex.getPropertyName() + " »");
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<Map<String, String>> handleInvalidDataAccess(InvalidDataAccessApiUsageException ex) {
        if (ex.getCause() instanceof PropertyReferenceException) {
            return handlePropertyReference((PropertyReferenceException) ex.getCause());
        }
        return handleGeneric(ex);
    }

    // Refus de @PreAuthorize (rôle insuffisant, RS-02) : levé dans le contrôleur,
    // il n'atteint jamais LoggingAccessDeniedHandler, d'où la journalisation ici.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        journaliserRefus(request, "rôle insuffisant");
        return buildResponse(HttpStatus.FORBIDDEN, "Accès refusé");
    }

    /**
     * Journalise un refus 403 avec l'utilisateur authentifié. Le contexte de
     * sécurité est encore disponible ici, mais plus dans SecurityAuditFilter
     * (qui entoure la chaîne Spring Security) : on lui transmet donc le nom
     * par un attribut de requête.
     */
    private void journaliserRefus(HttpServletRequest request, String motif) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String utilisateur = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)
                ? auth.getName()
                : "anonyme";
        request.setAttribute(SecurityAuditFilter.ATTRIBUT_UTILISATEUR, utilisateur);
        log.warn("[SECURITE] Accès refusé [403] - {} {} - Utilisateur: {} - Motif: {}",
                request.getMethod(), request.getRequestURI(), utilisateur, motif);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        // La trace reste masquée pour le client mais doit figurer dans les logs.
        log.error("Erreur inattendue", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne du serveur");
    }

    private ResponseEntity<Map<String, String>> buildResponse(HttpStatus status, String message) {
        Map<String, String> body = new HashMap<>();
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
