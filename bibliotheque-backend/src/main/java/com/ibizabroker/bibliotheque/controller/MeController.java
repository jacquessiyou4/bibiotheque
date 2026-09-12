package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Utilisateur courant")
@CrossOrigin("http://localhost:4200/")
@RestController
public class MeController {

    @Autowired
    private UsersRepository usersRepository;

    @Operation(summary = "Obtenir l'utilisateur local associé au jeton Keycloak")
    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        String username = authentication.getName();
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException(
                        "Aucun utilisateur local pour le compte Keycloak « " + username + " »."));

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getUserId());
        result.put("username", user.getUsername());
        result.put("name", user.getName());
        result.put("role", user.getRole());
        return result;
    }
}