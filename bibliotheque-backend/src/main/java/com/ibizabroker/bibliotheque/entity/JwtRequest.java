package com.ibizabroker.bibliotheque.entity;

public class JwtRequest {

    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    // Setters standards : Jackson lie les champs JSON « username » /
    // « password » via setUsername / setPassword.
    public void setUsername(String username) {
        this.username = username;
    }

    public void setUserName(String userName) {
        this.username = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUserPassword(String userPassword) {
        this.password = userPassword;
    }
}
