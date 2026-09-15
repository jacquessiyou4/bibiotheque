package com.ibizabroker.bibliotheque.utilisateurs.api;

/** Vue d'un utilisateur exposée aux autres fonctionnalités (sans mot de passe ni rôles). */
public final class UtilisateurResume {

    private final Integer userId;
    private final String username;
    private final String name;

    public UtilisateurResume(Integer userId, String username, String name) {
        this.userId = userId;
        this.username = username;
        this.name = name;
    }

    public Integer getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getName() { return name; }
}
