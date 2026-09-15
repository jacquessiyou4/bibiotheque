package com.ibizabroker.bibliotheque.shared.web;

import com.ibizabroker.bibliotheque.shared.error.BadRequestException;
import com.ibizabroker.bibliotheque.shared.error.ConflictException;
import com.ibizabroker.bibliotheque.shared.error.ForbiddenException;
import com.ibizabroker.bibliotheque.shared.error.NotFoundException;
import com.ibizabroker.bibliotheque.shared.error.ServiceUnavailableException;
import com.ibizabroker.bibliotheque.shared.error.UnauthorizedException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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

/**
 * Traduit chaque exception en réponse application/problem+json (voir
 * ProblemeHttp) : même forme pour toutes les erreurs, avec un code stable et
 * le X-Request-ID de la requête.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        return reponse(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException ex) {
        return reponse(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        journaliserRefus(request, ex.getMessage());
        return reponse(HttpStatus.FORBIDDEN, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException ex) {
        return reponse(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage());
    }

    // Règle garantie par la base (V4) : doublon d'username ou de réservation
    // active, ou suppression d'une donnée encore référencée (livre emprunté).
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Contrainte d'intégrité refusée : {}", ex.getMostSpecificCause().getMessage());
        return reponse(HttpStatus.CONFLICT, "DATA_CONFLICT",
                "Opération refusée : elle créerait un doublon ou toucherait une donnée encore utilisée.");
    }

    // Verrouillage optimiste : la donnée a changé depuis sa lecture.
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return reponse(HttpStatus.CONFLICT, "CONCURRENT_UPDATE",
                "Cette donnée vient d'être modifiée par quelqu'un d'autre. Rechargez la page puis réessayez.");
    }

    // Identifiants refusés par Keycloak (POST /auth/token).
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        return reponse(HttpStatus.UNAUTHORIZED, ex.getCode(), ex.getMessage());
    }

    // Keycloak injoignable ou en erreur : ce n'est pas une erreur du client.
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleServiceUnavailable(ServiceUnavailableException ex) {
        return reponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        // getFieldErrors() plutôt qu'un cast de getAllErrors() : une erreur
        // globale (niveau classe) provoquait une ClassCastException.
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage()));
        Map<String, Object> body = ProblemeHttp.corps(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Erreur de validation");
        body.put("errors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(ProblemeHttp.MEDIA_TYPE).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        return reponse(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", ex.getMessage());
    }

    // JSON mal formé ou paramètre au mauvais type (ex. ?statut=INCONNU) :
    // erreurs du client, pas des 500.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException ex) {
        return reponse(HttpStatus.BAD_REQUEST, "INVALID_BODY", "Corps de requête invalide");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return reponse(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "Valeur invalide pour le paramètre « " + ex.getName() + " »");
    }

    // Tri sur une propriété inconnue (ex. ?sortBy=inexistant) : Spring Data
    // lève PropertyReferenceException, parfois enveloppée par la traduction
    // d'exceptions de persistance.
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<Map<String, Object>> handlePropertyReference(PropertyReferenceException ex) {
        return reponse(HttpStatus.BAD_REQUEST, "INVALID_SORT",
                "Propriété de tri inconnue : « " + ex.getPropertyName() + " »");
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidDataAccess(InvalidDataAccessApiUsageException ex) {
        if (ex.getCause() instanceof PropertyReferenceException) {
            return handlePropertyReference((PropertyReferenceException) ex.getCause());
        }
        return handleGeneric(ex);
    }

    // Refus de @PreAuthorize (rôle insuffisant, RS-02) : levé dans le contrôleur,
    // il n'atteint jamais LoggingAccessDeniedHandler, d'où la journalisation ici.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        journaliserRefus(request, "rôle insuffisant");
        return reponse(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Accès refusé");
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
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        // La trace reste masquée pour le client mais doit figurer dans les logs,
        // retrouvable grâce au requestId renvoyé dans la réponse.
        log.error("Erreur inattendue", ex);
        return reponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Erreur interne du serveur");
    }

    private ResponseEntity<Map<String, Object>> reponse(HttpStatus status, String code, String detail) {
        return ResponseEntity.status(status)
                .contentType(ProblemeHttp.MEDIA_TYPE)
                .body(ProblemeHttp.corps(status, code, detail));
    }
}
