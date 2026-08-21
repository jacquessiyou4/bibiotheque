package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dao.LivreRepository;
import com.ibizabroker.bibliotheque.entity.Livre;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Livres")
@CrossOrigin("http://localhost:4200/")
@RestController
@RequestMapping("/admin/livres")
public class LivreController {

    @Autowired
    private LivreRepository livreRepository;

    @Operation(summary = "Lister les livres", description = "Renvoie tous les livres. Accessible à tout utilisateur authentifié.")
    @GetMapping
    public List<Livre> getAllLivres() {
        return livreRepository.findAll();
    }

    @Operation(summary = "Consulter un livre", description = "Renvoie un livre par son id. Accès réservé aux administrateurs.")
    @PreAuthorize("hasRole('Admin')")
    @GetMapping("/{id}")
    public ResponseEntity<Livre> getLivreById(@PathVariable Integer id) {
        Livre livre = livreRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Livre with id " + id + " does not exist."));
        return ResponseEntity.ok(livre);
    }

    @Operation(summary = "Créer un livre", description = "Ajoute un nouveau livre au catalogue. Accès réservé aux administrateurs.")
    @PreAuthorize("hasRole('Admin')")
    @PostMapping
    public Livre createLivre(@RequestBody Livre livre) {
        return livreRepository.save(livre);
    }

    @Operation(summary = "Modifier un livre", description = "Met à jour le titre, l'auteur, le genre et le nombre de copies d'un livre. Accès réservé aux administrateurs.")
    @PreAuthorize("hasRole('Admin')")
    @PutMapping("/{id}")
    public ResponseEntity<Livre> updateLivre(@PathVariable Integer id, @RequestBody Livre details) {
        Livre livre = livreRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Livre with id " + id + " does not exist."));

        livre.setBookName(details.getBookName());
        livre.setBookAuthor(details.getBookAuthor());
        livre.setBookGenre(details.getBookGenre());
        livre.setNoOfCopies(details.getNoOfCopies());

        return ResponseEntity.ok(livreRepository.save(livre));
    }

    @Operation(summary = "Supprimer un livre", description = "Supprime définitivement un livre du catalogue. Accès réservé aux administrateurs.")
    @PreAuthorize("hasRole('Admin')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteLivre(@PathVariable Integer id) {
        Livre livre = livreRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Livre with id " + id + " does not exist."));

        livreRepository.delete(livre);
        Map<String, Boolean> response = new HashMap<>();
        response.put("deleted", Boolean.TRUE);
        return ResponseEntity.ok(response);
    }
}
