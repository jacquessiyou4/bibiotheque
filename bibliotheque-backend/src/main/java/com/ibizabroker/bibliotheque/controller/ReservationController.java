package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dto.ReservationRequest;
import com.ibizabroker.bibliotheque.dto.ReservationResponse;
import com.ibizabroker.bibliotheque.entity.StatutReservation;
import com.ibizabroker.bibliotheque.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Réservations")
@Slf4j
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Operation(summary = "Créer une réservation (un ADHERENT seulement pour lui-même, RS-04)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Réservation créée"),
            @ApiResponse(responseCode = "400", description = "livreId ou adherentId manquant"),
            @ApiResponse(responseCode = "403", description = "Action non autorisée pour ce rôle"),
            @ApiResponse(responseCode = "404", description = "Livre ou adhérent introuvable"),
            @ApiResponse(responseCode = "409", description = "Règle de gestion violée (RG-01, RG-02 ou RG-03)")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADHERENT', 'BIBLIOTHECAIRE')")
    public ResponseEntity<ReservationResponse> creer(Authentication authentication,
                                                     @RequestBody ReservationRequest request) {
        ReservationResponse response = reservationService.creer(request,
                estBibliothecaire(authentication), authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Lister les réservations (un ADHERENT ne voit que les siennes, RS-05)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des réservations")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADHERENT', 'BIBLIOTHECAIRE')")
    public ResponseEntity<List<ReservationResponse>> lister(Authentication authentication,
            @RequestParam(required = false) StatutReservation statut,
            @RequestParam(required = false) Integer adherentId) {
        return ResponseEntity.ok(reservationService.lister(statut, adherentId,
                estBibliothecaire(authentication), authentication.getName()));
    }

    @Operation(summary = "Lister les réservations expirées (bibliothécaire uniquement)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservations expirées")
    })
    @GetMapping("/expirees")
    @PreAuthorize("hasRole('BIBLIOTHECAIRE')")
    public ResponseEntity<List<ReservationResponse>> listerExpirees() {
        return ResponseEntity.ok(reservationService.listerExpirees());
    }

    @Operation(summary = "Consulter une réservation (un ADHERENT uniquement la sienne, RS-03)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservation trouvée"),
            @ApiResponse(responseCode = "403", description = "Réservation d'un autre adhérent"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADHERENT', 'BIBLIOTHECAIRE')")
    public ResponseEntity<ReservationResponse> consulter(Authentication authentication,
                                                         @PathVariable Integer id) {
        return ResponseEntity.ok(reservationService.consulter(id,
                estBibliothecaire(authentication), authentication.getName()));
    }

    @Operation(summary = "Annuler une réservation (un ADHERENT uniquement la sienne, RS-03)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservation annulée"),
            @ApiResponse(responseCode = "403", description = "Réservation d'un autre adhérent"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable"),
            @ApiResponse(responseCode = "409", description = "Statut ne permettant pas l'annulation (RG-05 / RG-06)")
    })
    @PatchMapping("/{id}/annuler")
    @PreAuthorize("hasAnyRole('ADHERENT', 'BIBLIOTHECAIRE')")
    public ResponseEntity<ReservationResponse> annuler(Authentication authentication,
                                                       @PathVariable Integer id) {
        return ResponseEntity.ok(reservationService.annuler(id,
                estBibliothecaire(authentication), authentication.getName()));
    }

    @Operation(summary = "Supprimer une réservation (bibliothécaire uniquement)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Réservation supprimée"),
            @ApiResponse(responseCode = "403", description = "Action réservée au bibliothécaire (RS-02)"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BIBLIOTHECAIRE')")
    public ResponseEntity<Void> supprimer(@PathVariable Integer id) {
        reservationService.supprimer(id);
        return ResponseEntity.noContent().build();
    }

    private boolean estBibliothecaire(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_BIBLIOTHECAIRE".equals(authority.getAuthority()));
    }
}