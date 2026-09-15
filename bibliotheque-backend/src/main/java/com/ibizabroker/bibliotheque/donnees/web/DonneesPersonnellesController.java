package com.ibizabroker.bibliotheque.donnees.web;

import com.ibizabroker.bibliotheque.utilisateurs.api.UserResponse;
import com.ibizabroker.bibliotheque.donnees.internal.DonneesPersonnellesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "Données personnelles", description = "Export et effacement des données (RGPD)")
@Slf4j
@RestController
public class DonneesPersonnellesController {

    private final DonneesPersonnellesService donneesPersonnellesService;

    public DonneesPersonnellesController(DonneesPersonnellesService donneesPersonnellesService) {
        this.donneesPersonnellesService = donneesPersonnellesService;
    }

    @Operation(summary = "Télécharger mes données (profil, emprunts, réservations)")
    @GetMapping("/api/v1/profile/export")
    public ResponseEntity<DonneesPersonnellesResponse> exporter(Authentication authentication) {
        DonneesPersonnellesResponse donnees = donneesPersonnellesService.exporter(authentication);
        log.info("[DONNEES] Export des données personnelles - userId={} - par {}",
                donnees.getProfil().getUserId(), authentication.getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mes-donnees-bibliotheque.json\"")
                .body(donnees);
    }

    @Operation(summary = "Anonymiser un compte (droit à l'effacement, irréversible)")
    @PreAuthorize("hasRole('Admin')")
    @PostMapping("/api/v1/users/{id}/anonymisation")
    public UserResponse anonymiser(Authentication authentication, @PathVariable Integer id) {
        UserResponse anonyme = donneesPersonnellesService.anonymiser(id);
        log.warn("[DONNEES] Anonymisation du compte userId={} - par {}", id, authentication.getName());
        return anonyme;
    }
}
