package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.entity.Borrow;
import com.ibizabroker.bibliotheque.exceptions.ForbiddenException;
import com.ibizabroker.bibliotheque.service.BorrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * Emprunts. Un utilisateur (User / ADHERENT) n'agit que sur ses propres
 * emprunts ; l'Admin / BIBLIOTHECAIRE voit et gère ceux de tout le monde.
 * Sans ce contrôle, n'importe quel compte pouvait emprunter au nom d'un
 * autre, rendre ses livres ou lister tous les emprunts.
 */
@Tag(name = "Emprunts", description = "Gestion des emprunts de livres")
@Slf4j
@RestController
@RequestMapping("/borrow")
public class BorrowController {

    private final BorrowService borrowService;

    public BorrowController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @Operation(summary = "Emprunter un livre (un User uniquement pour lui-même)")
    @PreAuthorize("hasAnyRole('User', 'Admin', 'ADHERENT', 'BIBLIOTHECAIRE')")
    @PostMapping
    public String borrowBook(Authentication authentication, @Valid @RequestBody Borrow borrow) {
        if (!estPersonnel(authentication)
                && !borrowService.resolveUserId(authentication.getName()).equals(borrow.getUserId())) {
            throw new ForbiddenException("Accès refusé : vous ne pouvez emprunter que pour votre propre compte.");
        }
        return borrowService.borrowBook(borrow);
    }

    @Operation(summary = "Lister tous les emprunts (Admin / bibliothécaire)")
    @PreAuthorize("hasAnyRole('Admin', 'BIBLIOTHECAIRE')")
    @GetMapping
    public List<Borrow> getAllBorrow() {
        return borrowService.findAll();
    }

    @Operation(summary = "Retourner un livre emprunté (un User uniquement le sien)")
    @PreAuthorize("hasAnyRole('User', 'Admin', 'ADHERENT', 'BIBLIOTHECAIRE')")
    @PutMapping
    public Borrow returnBook(Authentication authentication, @Valid @RequestBody Borrow borrow) {
        Integer proprietaireAttendu = estPersonnel(authentication)
                ? null
                : borrowService.resolveUserId(authentication.getName());
        return borrowService.returnBook(borrow, proprietaireAttendu);
    }

    @Operation(summary = "Lister les emprunts d'un utilisateur (un User uniquement les siens)")
    @PreAuthorize("hasAnyRole('User', 'Admin', 'ADHERENT', 'BIBLIOTHECAIRE')")
    @GetMapping("user/{id}")
    public List<Borrow> booksBorrowedByUser(Authentication authentication, @PathVariable Integer id) {
        if (!estPersonnel(authentication)
                && !borrowService.resolveUserId(authentication.getName()).equals(id)) {
            throw new ForbiddenException("Accès refusé : ces emprunts ne vous appartiennent pas.");
        }
        return borrowService.findByUserId(id);
    }

    @Operation(summary = "Historique des emprunts d'un livre (Admin / bibliothécaire)")
    @PreAuthorize("hasAnyRole('Admin', 'BIBLIOTHECAIRE')")
    @GetMapping("book/{id}")
    public List<Borrow> bookBorrowHistory(@PathVariable Integer id) {
        return borrowService.findByBookId(id);
    }

    private boolean estPersonnel(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_Admin".equals(a.getAuthority())
                        || "ROLE_BIBLIOTHECAIRE".equals(a.getAuthority()));
    }
}
