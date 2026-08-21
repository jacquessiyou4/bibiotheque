package com.ibizabroker.bibliotheque.entity;

public class JwtResponse {

    private Integer id;
    private String username;
    private String name;
    private String matricule;
    private String role;
    private String jwtToken;
    private String refreshToken;

    public JwtResponse(Integer id, String username, String name, String matricule, String role, String jwtToken, String refreshToken) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.matricule = matricule;
        this.role = role;
        this.jwtToken = jwtToken;
        this.refreshToken = refreshToken;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMatricule() {
        return matricule;
    }

    public void setMatricule(String matricule) {
        this.matricule = matricule;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
