package com.ibizabroker.bibliotheque.shared.web;

import com.ibizabroker.bibliotheque.catalogue.internal.BooksRepository;
import com.ibizabroker.bibliotheque.emprunts.internal.BorrowRepository;
import com.ibizabroker.bibliotheque.reservations.internal.ReservationRepository;
import com.ibizabroker.bibliotheque.utilisateurs.internal.UsersRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.metrics.AutoConfigureMetrics;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prometheus lit /actuator/prometheus avec ses identifiants HTTP Basic ; toute
 * autre personne sans jeton administrateur est refusée.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
        "app.keycloak.jwks-uri=http://localhost:9999/realms/bibliotheque/protocol/openid-connect/certs",
        "app.keycloak.issuer-local=http://localhost:9999/realms/bibliotheque",
        "app.keycloak.issuer-internal=http://localhost:9999/realms/bibliotheque",
        "app.metriques.mot-de-passe=secret-de-test"
})
@AutoConfigureMockMvc
@AutoConfigureMetrics
class AccesMetriquesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BooksRepository booksRepository;

    @MockBean
    private BorrowRepository borrowRepository;

    @MockBean
    private ReservationRepository reservationRepository;

    @MockBean
    private UsersRepository usersRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void prometheus_avecSesIdentifiants_litLesMetriquesMetier() throws Exception {
        mockMvc.perform(get("/actuator/prometheus").header("Authorization", basic("prometheus", "secret-de-test")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("bibliotheque_emprunts_total")))
                .andExpect(content().string(containsString("bibliotheque_connexions_total{resultat=\"echec\"")))
                .andExpect(content().string(containsString("http_server_requests_seconds_bucket")));
    }

    @Test
    void mauvaisMotDePasse_ouSansIdentifiants_renvoie401() throws Exception {
        mockMvc.perform(get("/actuator/prometheus").header("Authorization", basic("prometheus", "faux")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lesIdentifiantsDePrometheus_nOuvrentQueLesMetriques() throws Exception {
        mockMvc.perform(get("/actuator/metrics").header("Authorization", basic("prometheus", "secret-de-test")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sansMotDePasseConfigure_lAccesBasicEstFerme() {
        AccesMetriques acces = new AccesMetriques("prometheus", "");
        MockHttpServletRequest requete = new MockHttpServletRequest();
        requete.addHeader("Authorization", basic("prometheus", ""));

        assertThat(acces.autorise(requete)).isFalse();
    }

    @Test
    void enTeteInvalide_estRefuse() {
        AccesMetriques acces = new AccesMetriques("prometheus", "secret");
        MockHttpServletRequest bearer = new MockHttpServletRequest();
        bearer.addHeader("Authorization", "Bearer jeton");
        MockHttpServletRequest base64Invalide = new MockHttpServletRequest();
        base64Invalide.addHeader("Authorization", "Basic %%%");

        assertThat(acces.autorise(bearer)).isFalse();
        assertThat(acces.autorise(base64Invalide)).isFalse();
        assertThat(acces.autorise(new MockHttpServletRequest())).isFalse();
    }

    private static String basic(String utilisateur, String motDePasse) {
        return "Basic " + Base64.getEncoder().encodeToString((utilisateur + ":" + motDePasse).getBytes(StandardCharsets.UTF_8));
    }
}
