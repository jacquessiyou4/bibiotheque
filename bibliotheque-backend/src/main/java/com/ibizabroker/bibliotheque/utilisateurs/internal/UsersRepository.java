package com.ibizabroker.bibliotheque.utilisateurs.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<Users, Integer> {

    Optional<Users> findByUsername(String username);

    Optional<Users> findByKeycloakSub(String keycloakSub);

    /** Relie un compte local encore libre à un compte Keycloak ; 0 si déjà relié entre-temps. */
    @Transactional
    @Modifying
    @Query("UPDATE Users u SET u.keycloakSub = :sub WHERE u.userId = :userId AND u.keycloakSub IS NULL")
    int lierCompteKeycloak(@Param("userId") Integer userId, @Param("sub") String sub);

    /** Suit un renommage fait dans Keycloak. */
    @Transactional
    @Modifying
    @Query("UPDATE Users u SET u.username = :username WHERE u.userId = :userId")
    int renommer(@Param("userId") Integer userId, @Param("username") String username);
}
