package com.ibizabroker.bibliotheque.shared.web;

import com.ibizabroker.bibliotheque.shared.error.BadRequestException;
import com.ibizabroker.bibliotheque.shared.error.ConflictException;
import com.ibizabroker.bibliotheque.shared.error.ForbiddenException;
import com.ibizabroker.bibliotheque.shared.error.NotFoundException;

import com.ibizabroker.bibliotheque.reservations.api.ReservationStatus;
import com.ibizabroker.bibliotheque.utilisateurs.internal.Users;
import org.junit.jupiter.api.AfterEach;
import org.slf4j.MDC;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.validation.ConstraintViolationException;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires de GlobalExceptionHandler : chaque exception est traduite
 * dans le bon code HTTP avec un message clair, sans trace technique, et les
 * refus 403 sont journalisés avec l'utilisateur à l'origine de la tentative.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest requete;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        requete = new MockHttpServletRequest("DELETE", "/api/reservations/3");
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------
    // Exceptions métier
    // ------------------------------------------------------------------
    @Test
    void handleNotFound_renvoie404AvecLeMessage() {
        ResponseEntity<Map<String, Object>> reponse = handler.handleNotFound(new NotFoundException("Livre introuvable"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(reponse.getBody()).containsEntry("message", "Livre introuvable");
    }

    @Test
    void handleBadRequest_renvoie400AvecLeMessage() {
        ResponseEntity<Map<String, Object>> reponse = handler.handleBadRequest(new BadRequestException("Stock épuisé"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reponse.getBody()).containsEntry("message", "Stock épuisé");
    }

    @Test
    void handleConflict_renvoie409AvecLeMessage() {
        ResponseEntity<Map<String, Object>> reponse = handler.handleConflict(new ConflictException("RG-01"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(reponse.getBody()).containsEntry("message", "RG-01");
    }

    @Test
    void handleDataIntegrity_renvoie409SansDetailSql() {
        ResponseEntity<Map<String, Object>> reponse = handler.handleDataIntegrity(new DataIntegrityViolationException(
                "could not execute statement",
                new IllegalStateException("duplicate key value violates unique constraint \"ux_users_username_lower\"")));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(reponse.getBody().get("message")).asString().contains("doublon");
        assertThat(reponse.getBody().toString()).doesNotContain("ux_users_username_lower");
    }

    @Test
    void handleOptimisticLock_renvoie409AvecUneConsigne() {
        ResponseEntity<Map<String, Object>> reponse = handler.handleOptimisticLock(
                new ObjectOptimisticLockingFailureException(Users.class, 1));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(reponse.getBody().get("message")).asString().contains("Rechargez");
    }

    // ------------------------------------------------------------------
    // Refus 403 : réponse + journalisation de l'utilisateur
    // ------------------------------------------------------------------
    @Test
    void handleForbidden_utilisateurAuthentifie_renvoie403EtTransmetSonNomALAudit() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "a1", null, AuthorityUtils.createAuthorityList("ROLE_ADHERENT")));

        ResponseEntity<Map<String, Object>> reponse = handler.handleForbidden(
                new ForbiddenException("Accès refusé : cette réservation ne vous appartient pas."), requete);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(reponse.getBody().get("message")).asString().contains("ne vous appartient pas");
        assertThat(requete.getAttribute(SecurityAuditFilter.ATTRIBUT_UTILISATEUR)).isEqualTo("a1");
    }

    @Test
    void handleAccessDenied_roleInsuffisant_renvoie403GeneriqueAvecLUtilisateur() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "a1", null, AuthorityUtils.createAuthorityList("ROLE_ADHERENT")));

        ResponseEntity<Map<String, Object>> reponse = handler.handleAccessDenied(
                new AccessDeniedException("Access is denied"), requete);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(reponse.getBody()).containsEntry("message", "Accès refusé");
        assertThat(requete.getAttribute(SecurityAuditFilter.ATTRIBUT_UTILISATEUR)).isEqualTo("a1");
    }

    @Test
    void handleAccessDenied_sansAuthentification_journaliseAnonyme() {
        handler.handleAccessDenied(new AccessDeniedException("Access is denied"), requete);

        assertThat(requete.getAttribute(SecurityAuditFilter.ATTRIBUT_UTILISATEUR)).isEqualTo("anonyme");
    }

    @Test
    void handleAccessDenied_jetonAnonymeSpring_journaliseAnonyme() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "cle", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        handler.handleAccessDenied(new AccessDeniedException("Access is denied"), requete);

        assertThat(requete.getAttribute(SecurityAuditFilter.ATTRIBUT_UTILISATEUR)).isEqualTo("anonyme");
    }

    @Test
    void handleAccessDenied_authentificationNonValidee_journaliseAnonyme() {
        // Jeton construit sans autorités : isAuthenticated() == false.
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("a1", "secret"));

        handler.handleAccessDenied(new AccessDeniedException("Access is denied"), requete);

        assertThat(requete.getAttribute(SecurityAuditFilter.ATTRIBUT_UTILISATEUR)).isEqualTo("anonyme");
    }

    // ------------------------------------------------------------------
    // Erreurs du client -> 400
    // ------------------------------------------------------------------
    @Test
    void handleValidation_renvoie400AvecLesErreursParChamp() {
        BeanPropertyBindingResult resultat = new BeanPropertyBindingResult(new Object(), "user");
        resultat.addError(new FieldError("user", "username", "Le nom d'utilisateur est obligatoire"));

        ResponseEntity<Map<String, Object>> reponse = handler.handleValidation(
                new MethodArgumentNotValidException((MethodParameter) null, resultat));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reponse.getBody()).containsEntry("message", "Erreur de validation");
        @SuppressWarnings("unchecked")
        Map<String, String> erreurs = (Map<String, String>) reponse.getBody().get("errors");
        assertThat(erreurs).containsEntry("username", "Le nom d'utilisateur est obligatoire");
    }

    @Test
    void handleConstraintViolation_renvoie400() {
        ResponseEntity<Map<String, Object>> reponse = handler.handleConstraintViolation(
                new ConstraintViolationException("id: doit être positif", Collections.emptySet()));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reponse.getBody()).containsEntry("message", "id: doit être positif");
    }

    @Test
    void handleNotReadable_jsonMalForme_renvoie400SansDetailTechnique() {
        ResponseEntity<Map<String, Object>> reponse = handler.handleNotReadable(
                new HttpMessageNotReadableException("JSON parse error: Unexpected character", (HttpInputMessage) null));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reponse.getBody()).containsEntry("message", "Corps de requête invalide");
    }

    @Test
    void handleTypeMismatch_nommeLeParametreInvalide() {
        ResponseEntity<Map<String, Object>> reponse = handler.handleTypeMismatch(
                new MethodArgumentTypeMismatchException("INCONNU", ReservationStatus.class, "statut", null, null));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reponse.getBody().get("message")).asString().contains("statut");
    }

    @Test
    void handlePropertyReference_triInconnu_renvoie400AvecLaPropriete() {
        ResponseEntity<Map<String, Object>> reponse = handler.handlePropertyReference(proprieteInconnue());

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reponse.getBody().get("message")).asString().contains("inexistant");
    }

    @Test
    void handleInvalidDataAccess_causeTriInconnu_renvoie400() {
        PropertyReferenceException cause = proprieteInconnue();

        ResponseEntity<Map<String, Object>> reponse = handler.handleInvalidDataAccess(
                new InvalidDataAccessApiUsageException(cause.getMessage(), cause));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(reponse.getBody().get("message")).asString().contains("inexistant");
    }

    @Test
    void handleInvalidDataAccess_autreCause_renvoie500() {
        ResponseEntity<Map<String, Object>> reponse = handler.handleInvalidDataAccess(
                new InvalidDataAccessApiUsageException("usage invalide", new IllegalStateException("détail interne")));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(reponse.getBody()).containsEntry("message", "Erreur interne du serveur");
    }

    // ------------------------------------------------------------------
    // Erreur inattendue -> 500 sans fuite de détail
    // ------------------------------------------------------------------
    @Test
    void handleGeneric_masqueLeDetailTechniqueAuClient() {
        ResponseEntity<Map<String, Object>> reponse = handler.handleGeneric(
                new IllegalStateException("NullPointer dans ReservationService ligne 42"));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(reponse.getBody()).containsEntry("message", "Erreur interne du serveur");
        assertThat(reponse.getBody().toString()).doesNotContain("ReservationService");
    }

    // ------------------------------------------------------------------
    // Format commun application/problem+json
    // ------------------------------------------------------------------
    @Test
    void reponse_suitLeFormatProblemJsonAvecCodeEtRequestId() {
        MDC.put(ProblemeHttp.MDC_REQUEST_ID, "req-123");
        try {
            ResponseEntity<Map<String, Object>> reponse = handler.handleConflict(
                    new ConflictException("RESERVATION_DUPLICATE", "RG-02: déjà réservé"));

            assertThat(reponse.getHeaders().getContentType()).isEqualTo(ProblemeHttp.MEDIA_TYPE);
            assertThat(reponse.getBody())
                    .containsEntry("status", 409)
                    .containsEntry("title", "Conflict")
                    .containsEntry("code", "RESERVATION_DUPLICATE")
                    .containsEntry("detail", "RG-02: déjà réservé")
                    .containsEntry("message", "RG-02: déjà réservé")
                    .containsEntry("type", "https://bibliotheque.local/problemes/reservation-duplicate")
                    .containsEntry("requestId", "req-123")
                    .containsKey("timestamp");
        } finally {
            MDC.remove(ProblemeHttp.MDC_REQUEST_ID);
        }
    }

    @Test
    void exceptionSansCodeExplicite_utiliseLeCodeParDefautDuStatut() {
        ResponseEntity<Map<String, Object>> reponse = handler.handleNotFound(new NotFoundException("Livre introuvable"));

        assertThat(reponse.getBody()).containsEntry("code", "NOT_FOUND").doesNotContainKey("requestId");
    }

    private PropertyReferenceException proprieteInconnue() {
        return new PropertyReferenceException("inexistant", ClassTypeInformation.from(Users.class), Collections.emptyList());
    }
}
