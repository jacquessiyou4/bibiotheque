package com.ibizabroker.bibliotheque.utilisateurs.internal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Set;

@Data
@Entity
@Table(name = "users")
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;

    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    private String username;

    // Identifiant immuable du compte Keycloak (claim « sub »), voir CompteKeycloakFilter.
    @Column(name = "keycloak_sub", unique = true)
    private String keycloakSub;

    @NotBlank(message = "Le nom est obligatoire")
    private String name;

    @JsonIgnore
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String password;
    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "USER_ROLE",
            joinColumns = {
                    @JoinColumn(name = "USER_ID")
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "ROLE_ID")
            }
    )
    private Set<Role> role;

}

