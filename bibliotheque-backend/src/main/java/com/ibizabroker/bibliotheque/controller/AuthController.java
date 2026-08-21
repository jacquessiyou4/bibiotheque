package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dto.ForgotPasswordRequest;
import com.ibizabroker.bibliotheque.dto.ForgotPasswordResponse;
import com.ibizabroker.bibliotheque.dto.RefreshTokenRequest;
import com.ibizabroker.bibliotheque.dto.RegisterRequest;
import com.ibizabroker.bibliotheque.dto.RegisterResponse;
import com.ibizabroker.bibliotheque.dto.ResetPasswordRequest;
import com.ibizabroker.bibliotheque.entity.JwtRequest;
import com.ibizabroker.bibliotheque.entity.JwtResponse;
import com.ibizabroker.bibliotheque.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "Authentification")
@RestController
@CrossOrigin
public class AuthController {

    @Autowired
    private JwtService jwtService;

    @Operation(summary = "Créer un compte", description = "Crée un compte Administrator ou Adherent selon le champ \"role\" fourni. Accessible sans authentification.")
    @PostMapping("/register")
    public RegisterResponse register(@RequestBody RegisterRequest request) {
        return jwtService.register(request);
    }

    @Operation(summary = "Se connecter", description = "Authentifie un Administrator ou un Adherent et renvoie un token d'accès (jwtToken) ainsi qu'un refresh token.")
    @PostMapping("/login")
    public JwtResponse login(@RequestBody JwtRequest request) throws Exception {
        return jwtService.createJwtToken(request);
    }

    @Operation(summary = "Rafraîchir le token d'accès", description = "Échange un refresh token valide contre un nouveau token d'accès et un nouveau refresh token (rotation).")
    @PostMapping("/refresh-token")
    public JwtResponse refreshToken(@RequestBody RefreshTokenRequest request) throws Exception {
        return jwtService.refreshToken(request);
    }

    @Operation(summary = "Mot de passe oublié", description = "Génère un token de réinitialisation pour le nom d'utilisateur donné. Aucune infrastructure d'email n'existe : le token est renvoyé directement dans la réponse.")
    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return jwtService.forgotPassword(request.getUsername());
    }

    @Operation(summary = "Réinitialiser le mot de passe", description = "Définit un nouveau mot de passe à l'aide d'un token de réinitialisation obtenu via /forgot-password.")
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Boolean>> resetPassword(@RequestBody ResetPasswordRequest request) {
        jwtService.resetPassword(request);
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", Boolean.TRUE);
        return ResponseEntity.ok(response);
    }
}
