package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dao.AdministratorRepository;
import com.ibizabroker.bibliotheque.entity.Administrator;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Administrateurs")
@CrossOrigin("http://localhost:4200/")
@RestController
@RequestMapping("/admin/administrators")
@PreAuthorize("hasRole('Admin')")
public class AdministratorController {

    @Autowired
    private AdministratorRepository administratorRepository;

    @Operation(summary = "Lister les administrateurs", description = "Renvoie tous les comptes Administrator. Accès réservé aux administrateurs.")
    @GetMapping
    public List<Administrator> getAllAdministrators() {
        return administratorRepository.findAll();
    }

    @Operation(summary = "Consulter un administrateur", description = "Renvoie un administrateur par son id. Accès réservé aux administrateurs.")
    @GetMapping("/{id}")
    public ResponseEntity<Administrator> getAdministratorById(@PathVariable Integer id) {
        Administrator administrator = administratorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Administrator with id " + id + " does not exist."));
        return ResponseEntity.ok(administrator);
    }

    @Operation(summary = "Modifier un administrateur", description = "Met à jour le nom et le nom d'utilisateur d'un administrateur. Accès réservé aux administrateurs.")
    @PutMapping("/{id}")
    public ResponseEntity<Administrator> updateAdministrator(@PathVariable Integer id, @RequestBody Administrator details) {
        Administrator administrator = administratorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Administrator with id " + id + " does not exist."));

        administrator.setName(details.getName());
        administrator.setUsername(details.getUsername());

        return ResponseEntity.ok(administratorRepository.save(administrator));
    }
}
