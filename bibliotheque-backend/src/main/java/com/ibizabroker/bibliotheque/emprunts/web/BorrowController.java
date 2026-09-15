package com.ibizabroker.bibliotheque.emprunts.web;

import com.ibizabroker.bibliotheque.emprunts.api.BorrowResponse;
import com.ibizabroker.bibliotheque.emprunts.internal.Borrow;
import com.ibizabroker.bibliotheque.shared.error.ForbiddenException;
import com.ibizabroker.bibliotheque.emprunts.internal.BorrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Emprunts. Un utilisateur (User / ADHERENT) n'agit que sur ses propres
 * emprunts ; l'Admin / BIBLIOTHECAIRE voit et gère ceux de tout le monde.
 * Sans ce contrôle, n'importe quel compte pouvait emprunter au nom d'un
 * autre, rendre ses livres ou lister tous les emprunts.
 */
@Tag(name = "Emprunts", description = "Gestion des emprunts de livres")
@Slf4j
@RestController
@RequestMapping("/api/v1/loans")
public class BorrowController {

    private final BorrowService borrowService;

    public BorrowController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @Operation(summary = "Emprunter un livre (un User uniquement pour lui-même)")
    @PreAuthorize("hasAnyRole('User', 'Admin', 'ADHERENT', 'BIBLIOTHECAIRE')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BorrowResponse borrowBook(Authentication authentication, @Valid @RequestBody BorrowRequest request) {
        if (!estPersonnel(authentication)
                && !borrowService.resolveUserId(authentication.getName()).equals(request.getUserId())) {
            throw new ForbiddenException("BORROW_NOT_OWNED", "Accès refusé : vous ne pouvez emprunter que pour votre propre compte.");
        }
        Borrow emprunt = borrowService.borrowBook(request.getBookId(), request.getUserId());
        log.info("[EMPRUNT] Emprunt - livre={} - pour userId={} - par {}",
                request.getBookId(), request.getUserId(), authentication.getName());
        return BorrowResponse.from(emprunt);
    }

    @Operation(summary = "Lister tous les emprunts (Admin / bibliothécaire)")
    @PreAuthorize("hasAnyRole('Admin', 'BIBLIOTHECAIRE')")
    @GetMapping
    public List<BorrowResponse> getAllBorrow() {
        return versReponses(borrowService.findAll());
    }

    @Operation(summary = "Retourner un livre emprunté (un User uniquement le sien)")
    @PreAuthorize("hasAnyRole('User', 'Admin', 'ADHERENT', 'BIBLIOTHECAIRE')")
    @PutMapping
    public BorrowResponse returnBook(Authentication authentication, @Valid @RequestBody ReturnBorrowRequest request) {
        Integer proprietaireAttendu = estPersonnel(authentication)
                ? null
                : borrowService.resolveUserId(authentication.getName());
        Borrow rendu = borrowService.returnBook(request.getBorrowId(), proprietaireAttendu);
        log.info("[EMPRUNT] Retour - emprunt={} - livre={} - par {}",
                rendu.getBorrowId(), rendu.getBookId(), authentication.getName());
        return BorrowResponse.from(rendu);
    }

    @Operation(summary = "Lister les emprunts d'un utilisateur (un User uniquement les siens)")
    @PreAuthorize("hasAnyRole('User', 'Admin', 'ADHERENT', 'BIBLIOTHECAIRE')")
    @GetMapping("user/{id}")
    public List<BorrowResponse> booksBorrowedByUser(Authentication authentication, @PathVariable Integer id) {
        if (!estPersonnel(authentication)
                && !borrowService.resolveUserId(authentication.getName()).equals(id)) {
            throw new ForbiddenException("BORROW_NOT_OWNED", "Accès refusé : ces emprunts ne vous appartiennent pas.");
        }
        return versReponses(borrowService.findByUserId(id));
    }

    @Operation(summary = "Historique des emprunts d'un livre (Admin / bibliothécaire)")
    @PreAuthorize("hasAnyRole('Admin', 'BIBLIOTHECAIRE')")
    @GetMapping("book/{id}")
    public List<BorrowResponse> bookBorrowHistory(@PathVariable Integer id) {
        return versReponses(borrowService.findByBookId(id));
    }

    private List<BorrowResponse> versReponses(List<Borrow> emprunts) {
        return emprunts.stream().map(BorrowResponse::from).collect(Collectors.toList());
    }

    private boolean estPersonnel(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_Admin".equals(a.getAuthority())
                        || "ROLE_BIBLIOTHECAIRE".equals(a.getAuthority()));
    }
}
