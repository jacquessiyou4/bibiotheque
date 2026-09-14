package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.BorrowRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.ReservationStatus;
import com.ibizabroker.bibliotheque.entity.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration des règles de sécurité (RS-01 à RS-05) sur les
 * endpoints de réservation. Le décodage JWT est simulé (@MockBean JwtDecoder)
 * : Keycloak et la base de données ne sont pas nécessaires pour lancer les
 * tests, la conversion rôles -> autorités reste réalisée par le vrai
 * JwtAuthenticationConverter et la vraie couche service/controller.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
        "app.keycloak.jwks-uri=http://localhost:9999/realms/bibliotheque/protocol/openid-connect/certs",
        "app.keycloak.issuer-local=http://localhost:9999/realms/bibliotheque",
        "app.keycloak.issuer-internal=http://localhost:9999/realms/bibliotheque"
})
@AutoConfigureMockMvc
class ReservationControllerIntegrationTest {

    private static final String TOKEN_ADHERENT = "token-adherent";
    private static final String TOKEN_BIBLIOTHECAIRE = "token-bibliothecaire";
    private static final String TOKEN_EXPIRE = "token-expire";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservationRepository reservationRepository;

    @MockBean
    private BooksRepository booksRepository;

    @MockBean
    private UsersRepository usersRepository;

    @MockBean
    private BorrowRepository borrowRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    private Users adherentUn;
    private Users adherentDeux;
    private Users bibliothecaire;
    private Books livreIndisponible;
    private Reservation reservationUn;
    private Reservation reservationDeux;

    @BeforeEach
    void setUp() {
        adherentUn = utilisateur(1, "A1");
        adherentDeux = utilisateur(2, "A2");
        bibliothecaire = utilisateur(10, "admin");

        livreIndisponible = new Books();
        livreIndisponible.setBookId(3);
        livreIndisponible.setBookName("L2");
        livreIndisponible.setNoOfCopies(0);

        reservationUn = reservation(100, adherentUn, ReservationStatus.EN_ATTENTE);
        reservationDeux = reservation(101, adherentDeux, ReservationStatus.EN_ATTENTE);

        when(usersRepository.findByUsername("A1")).thenReturn(Optional.of(adherentUn));
        when(usersRepository.findByUsername("A2")).thenReturn(Optional.of(adherentDeux));
        when(usersRepository.findByUsername("admin")).thenReturn(Optional.of(bibliothecaire));
        when(usersRepository.findById(1)).thenReturn(Optional.of(adherentUn));
        when(usersRepository.findById(10)).thenReturn(Optional.of(bibliothecaire));
        when(usersRepository.findById(2)).thenReturn(Optional.of(adherentDeux));

        when(booksRepository.findById(3)).thenReturn(Optional.of(livreIndisponible));

        when(reservationRepository.findByAdherent_UserId(1))
                .thenReturn(Collections.singletonList(reservationUn));
        when(reservationRepository.findByAdherent_UserId(2))
                .thenReturn(Collections.singletonList(reservationDeux));
        when(reservationRepository.findAll())
                .thenReturn(Arrays.asList(reservationUn, reservationDeux));
        when(reservationRepository.findById(100)).thenReturn(Optional.of(reservationUn));
        when(reservationRepository.findById(101)).thenReturn(Optional.of(reservationDeux));
        when(reservationRepository.findByLivre_BookIdAndAdherent_UserIdAndStatutIn(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(reservationRepository.countByAdherent_UserIdAndStatutIn(anyInt(), any())).thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationRepository.findByStatutInAndDateExpirationBefore(any(), any()))
                .thenReturn(Collections.emptyList());

        when(jwtDecoder.decode(any(String.class))).thenAnswer(invocation -> {
            String token = invocation.getArgument(0);
            if (TOKEN_EXPIRE.equals(token)) {
                // Simule la validation d'un jeton expiré par le vrai JwtDecoder.
                throw new JwtException("Jwt expired at 2026-09-01T00:00:00Z, current time is 2026-09-11T00:00:00Z");
            }
            if (TOKEN_BIBLIOTHECAIRE.equals(token)) {
                return jwt("admin", Arrays.asList("Admin", "BIBLIOTHECAIRE"), token);
            }
            return jwt("A1", Arrays.asList("User", "ADHERENT"), token);
        });
    }

    // ------------------------------------------------------------------
    // RS-01 : sans token, tout endpoint de réservation renvoie 401
    // ------------------------------------------------------------------
    @Test
    void rs01_sansToken_getReservations_renvoie401() throws Exception {
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rs01_sansToken_getReservationParId_renvoie401() throws Exception {
        mockMvc.perform(get("/api/reservations/100"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rs01_sansToken_postCreerReservation_renvoie401() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType("application/json")
                        .content("{\"livreId\":3}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rs01_sansToken_patchAnnulerReservation_renvoie401() throws Exception {
        mockMvc.perform(patch("/api/reservations/100/annuler"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rs01_sansToken_deleteReservation_renvoie401() throws Exception {
        mockMvc.perform(delete("/api/reservations/100"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // Bonus : expiration du jeton — 401 avec un message explicite
    // ------------------------------------------------------------------
    @Test
    void rs01_avecTokenExpire_getReservations_renvoie401AvecMessageSessionExpiree() throws Exception {
        mockMvc.perform(get("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_EXPIRE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Votre session a expiré. Veuillez vous reconnecter."));
    }

    // ------------------------------------------------------------------
    // Endpoints accessibles à un ADHERENT (RS-05 : ses réservations)
    // ------------------------------------------------------------------
    @Test
    void rs05_avecTokenAdherent_getReservations_renvoie200EtSesReservations() throws Exception {
        mockMvc.perform(get("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADHERENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].adherentId").value(1));
    }

    @Test
    void rs03_avecTokenAdherent_getReservationDUnAutre_renvoie403() throws Exception {
        mockMvc.perform(get("/api/reservations/101")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADHERENT))
                .andExpect(status().isForbidden());
    }

    @Test
    void rs03_avecTokenAdherent_getSaPropreReservation_renvoie200() throws Exception {
        mockMvc.perform(get("/api/reservations/100")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADHERENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adherentId").value(1));
    }

    @Test
    void rs04_avecTokenAdherent_postCreerIgnoreAdherentIdDuCorps_renvoie201PourLui() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADHERENT)
                        .contentType("application/json")
                        .content("{\"livreId\":3,\"adherentId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adherentId").value(1));
    }

    @Test
    void rg01_avecTokenAdherent_postCreerLivreDisponible_renvoie409() throws Exception {
        livreIndisponible.setNoOfCopies(1);
        mockMvc.perform(post("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADHERENT)
                        .contentType("application/json")
                        .content("{\"livreId\":3}"))
                .andExpect(status().isConflict());
    }

    @Test
    void rs03_avecTokenAdherent_patchAnnulerReservationDUnAutre_renvoie403() throws Exception {
        mockMvc.perform(patch("/api/reservations/101/annuler")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADHERENT))
                .andExpect(status().isForbidden());
    }

    @Test
    void rs03_avecTokenAdherent_patchAnnulerSaReservation_renvoie200() throws Exception {
        mockMvc.perform(patch("/api/reservations/100/annuler")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADHERENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value(ReservationStatus.ANNULEE.name()));
    }

    @Test
    void rs02_avecTokenAdherent_deleteReservation_renvoie403() throws Exception {
        mockMvc.perform(delete("/api/reservations/100")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADHERENT))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Endpoints réservés au BIBLIOTHECAIRE
    // ------------------------------------------------------------------
    @Test
    void avecTokenBibliothecaire_getReservationDUnAutre_renvoie200() throws Exception {
        mockMvc.perform(get("/api/reservations/101")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_BIBLIOTHECAIRE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adherentId").value(2));
    }

    @Test
    void avecTokenBibliothecaire_getReservations_renvoieToutes() throws Exception {
        mockMvc.perform(get("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_BIBLIOTHECAIRE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void avecTokenBibliothecaire_postCreerPourUnAutre_renvoie201() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_BIBLIOTHECAIRE)
                        .contentType("application/json")
                        .content("{\"livreId\":3,\"adherentId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adherentId").value(2));
    }

    @Test
    void avecTokenBibliothecaire_patchAnnulerReservationDUnAutre_renvoie200() throws Exception {
        mockMvc.perform(patch("/api/reservations/101/annuler")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_BIBLIOTHECAIRE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value(ReservationStatus.ANNULEE.name()));
    }

    @Test
    void avecTokenBibliothecaire_deleteReservation_renvoie204() throws Exception {
        mockMvc.perform(delete("/api/reservations/100")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_BIBLIOTHECAIRE))
                .andExpect(status().isNoContent());
    }

    @Test
    void avecTokenAdherent_getReservationInexistante_renvoie404() throws Exception {
        mockMvc.perform(get("/api/reservations/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_ADHERENT))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------

    private Users utilisateur(int userId, String username) {
        Users user = new Users();
        user.setUserId(userId);
        user.setUsername(username);
        user.setName(username);
        return user;
    }

    private Reservation reservation(int id, Users adherent, ReservationStatus statut) {
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setLivre(livreIndisponible);
        reservation.setAdherent(adherent);
        reservation.setDateReservation(LocalDateTime.now());
        reservation.setDateExpiration(LocalDateTime.now().plusDays(7));
        reservation.setStatut(statut);
        return reservation;
    }

    private Jwt jwt(String username, List<String> roles, String token) {
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", roles);
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", username);
        claims.put("preferred_username", username);
        claims.put("realm_access", realmAccess);
        return new Jwt(token, Instant.now(), Instant.now().plusSeconds(300),
                Collections.singletonMap("alg", "none"), claims);
    }
}