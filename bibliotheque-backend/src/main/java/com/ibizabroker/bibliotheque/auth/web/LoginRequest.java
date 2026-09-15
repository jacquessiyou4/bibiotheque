package com.ibizabroker.bibliotheque.auth.web;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    @Schema(example = "A1")
    private String username;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Schema(example = "A1123")
    private String password;

    public LoginRequest() {}

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
