package com.ibizabroker.bibliotheque.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires de JwtUtil (JWT maison « legacy », jjwt, non lié à
 * Keycloak). Aucune base ni serveur : le jeton est généré puis relu en
 * mémoire avec la même clé secrète.
 */
class JwtUtilTest {

    private static final String SECRET_KEY = "learn_programming_yourself";
    private static final int TOKEN_VALIDITY = 3600 * 5;

    private JwtUtil jwtUtil;

    private UserDetails utilisateur;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // JwtUtil lit la clé via @Value("${jwt.secret}") : hors contexte Spring,
        // il faut l'injecter à la main (plus de valeur codée en dur).
        org.springframework.test.util.ReflectionTestUtils.setField(jwtUtil, "SECRET_KEY", SECRET_KEY);
        utilisateur = new User("A1", "mot-de-passe", Collections.emptyList());
    }

    @Test
    void generateToken_definitLeSujetAuNomDUtilisateur() {
        String token = jwtUtil.generateToken(utilisateur);

        assertThat(jwtUtil.getUsernameFromToken(token)).isEqualTo("A1");
    }

    @Test
    void generateToken_definitUneExpirationDEnvironCinqHeures() {
        Date avant = new Date();

        String token = jwtUtil.generateToken(utilisateur);

        Date expiration = jwtUtil.getExpirationDateFromToken(token);
        long dureeMs = expiration.getTime() - avant.getTime();
        // TOKEN_VALIDITY * 1000 ms, avec une petite marge d'horloge.
        assertThat(dureeMs)
                .isGreaterThan(TOKEN_VALIDITY * 1000L - 1000)
                .isLessThan(TOKEN_VALIDITY * 1000L + 5000);
    }

    @Test
    void validateToken_accepteLeJetonDuMemeUtilisateur() {
        String token = jwtUtil.generateToken(utilisateur);

        assertThat(jwtUtil.validateToken(token, utilisateur)).isTrue();
    }

    @Test
    void validateToken_refuseLeJetonDUnAutreUtilisateur() {
        String token = jwtUtil.generateToken(utilisateur);
        UserDetails autre = new User("A2", "autre", Collections.emptyList());

        assertThat(jwtUtil.validateToken(token, autre)).isFalse();
    }

    @Test
    void validateToken_refuseUnJetonExpiré() {
        // Jeton construit à la main avec une expiration déjà passée :
        // simule ce que renverrait generateToken 5 heures plus tard.
        String tokenExpiré = Jwts.builder()
                .setSubject("A1")
                .setIssuedAt(new Date(System.currentTimeMillis() - TOKEN_VALIDITY * 1000L))
                .setExpiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(SignatureAlgorithm.HS512, SECRET_KEY)
                .compact();

        // jjwt refuse de relire les claims d'un jeton expiré : l'erreur
        // ExpiredJwtException remonte, le jeton est donc bien invalidé.
        assertThatThrownBy(() -> jwtUtil.validateToken(tokenExpiré, utilisateur))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    void getClaimFromToken_permetDeLireUnClaimQuelconque() {
        String token = jwtUtil.generateToken(utilisateur);

        Date issuedAt = jwtUtil.getClaimFromToken(token, Claims::getIssuedAt);

        assertThat(issuedAt).isNotNull();
        assertThat(issuedAt).isAfterOrEqualsTo(new Date(System.currentTimeMillis() - 5000));
    }
}
