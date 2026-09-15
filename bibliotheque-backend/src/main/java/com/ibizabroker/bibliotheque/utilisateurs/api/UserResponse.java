package com.ibizabroker.bibliotheque.utilisateurs.api;

import java.util.List;

public class UserResponse {

    private long userId;
    private String username;
    private String name;
    private List<String> roles;

    public UserResponse() {}

    public UserResponse(long userId, String username, String name, List<String> roles) {
        this.userId = userId;
        this.username = username;
        this.name = name;
        this.roles = roles;
    }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
}
