package com.ibizabroker.bibliotheque.entity;

import io.swagger.v3.oas.annotations.media.Schema;

public class JwtRequest {

    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    @Schema(description = "Nom d'utilisateur du compte (Administrator ou Adherent). Exemple : admin")
    public void setUserName(String userName) {
        this.username = userName;
    }

    public String getPassword() {
        return password;
    }

    @Schema(description = "Mot de passe en clair du compte. Exemple : admin123")
    public void setUserPassword(String userPassword) {
        this.password = userPassword;
    }
}
