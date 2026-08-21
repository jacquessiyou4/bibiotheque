package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.entity.JwtRequest;
import com.ibizabroker.bibliotheque.entity.JwtResponse;
import com.ibizabroker.bibliotheque.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentification")
@RestController
@CrossOrigin
//@RequestMapping("/")
public class JwtController {

    @Autowired
    private JwtService jwtService;

    @Operation(summary = "Se connecter (alias historique)", description = "Alias historique de POST /login, conservé pour compatibilité. Renvoie un token d'accès et un refresh token.")
    @PostMapping("/authenticate")
    public JwtResponse createJwtToken(@RequestBody JwtRequest jwtRequest) throws Exception {
        return jwtService.createJwtToken(jwtRequest);
    }
}
