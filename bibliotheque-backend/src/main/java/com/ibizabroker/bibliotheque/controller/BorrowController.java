package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.entity.Borrow;
import com.ibizabroker.bibliotheque.service.BorrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "Emprunts", description = "Gestion des emprunts de livres")
@Slf4j
@RestController
@RequestMapping("/borrow")
public class BorrowController {

    private final BorrowService borrowService;

    public BorrowController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @Operation(summary = "Emprunter un livre")
    @PreAuthorize("hasAnyRole('User', 'Admin', 'ADHERENT', 'BIBLIOTHECAIRE')")
    @PostMapping
    public String borrowBook(@Valid @RequestBody Borrow borrow) {
        return borrowService.borrowBook(borrow);
    }

    @Operation(summary = "Lister tous les emprunts")
    @PreAuthorize("hasAnyRole('User', 'Admin', 'ADHERENT', 'BIBLIOTHECAIRE')")
    @GetMapping
    public List<Borrow> getAllBorrow() {
        return borrowService.findAll();
    }

    @Operation(summary = "Retourner un livre emprunté")
    @PreAuthorize("hasAnyRole('User', 'Admin', 'ADHERENT', 'BIBLIOTHECAIRE')")
    @PutMapping
    public Borrow returnBook(@Valid @RequestBody Borrow borrow) {
        return borrowService.returnBook(borrow);
    }

    @Operation(summary = "Lister les emprunts d'un utilisateur")
    @PreAuthorize("hasAnyRole('User', 'Admin', 'ADHERENT', 'BIBLIOTHECAIRE')")
    @GetMapping("user/{id}")
    public List<Borrow> booksBorrowedByUser(@PathVariable Integer id) {
        return borrowService.findByUserId(id);
    }

    @Operation(summary = "Historique des emprunts d'un livre")
    @PreAuthorize("hasAnyRole('User', 'Admin', 'ADHERENT', 'BIBLIOTHECAIRE')")
    @GetMapping("book/{id}")
    public List<Borrow> bookBorrowHistory(@PathVariable Integer id) {
        return borrowService.findByBookId(id);
    }

}
