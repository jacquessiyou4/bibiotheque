package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dao.AdherentRepository;
import com.ibizabroker.bibliotheque.entity.Adherent;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Adhérents")
@CrossOrigin("http://localhost:4200/")
@RestController
@RequestMapping("/admin/adherents")
@PreAuthorize("hasRole('Admin')")
public class AdherentController {

    @Autowired
    private AdherentRepository adherentRepository;

    @Operation(summary = "Lister les adhérents", description = "Renvoie tous les comptes Adherent. Accès réservé aux administrateurs.")
    @GetMapping
    public List<Adherent> getAllAdherents() {
        return adherentRepository.findAll();
    }

    @Operation(summary = "Consulter un adhérent", description = "Renvoie un adhérent par son id. Accès réservé aux administrateurs.")
    @GetMapping("/{id}")
    public ResponseEntity<Adherent> getAdherentById(@PathVariable Integer id) {
        Adherent adherent = adherentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Adherent with id " + id + " does not exist."));
        return ResponseEntity.ok(adherent);
    }

    @Operation(summary = "Modifier un adhérent", description = "Met à jour le nom et le nom d'utilisateur d'un adhérent. Accès réservé aux administrateurs.")
    @PutMapping("/{id}")
    public ResponseEntity<Adherent> updateAdherent(@PathVariable Integer id, @RequestBody Adherent details) {
        Adherent adherent = adherentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Adherent with id " + id + " does not exist."));

        adherent.setName(details.getName());
        adherent.setUsername(details.getUsername());

        return ResponseEntity.ok(adherentRepository.save(adherent));
    }
}
