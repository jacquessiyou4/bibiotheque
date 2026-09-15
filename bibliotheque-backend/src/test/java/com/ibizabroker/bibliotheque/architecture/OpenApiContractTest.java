package com.ibizabroker.bibliotheque.architecture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibizabroker.bibliotheque.catalogue.internal.BooksRepository;
import com.ibizabroker.bibliotheque.emprunts.internal.BorrowRepository;
import com.ibizabroker.bibliotheque.reservations.internal.ReservationRepository;
import com.ibizabroker.bibliotheque.utilisateurs.internal.UsersRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Écrit le contrat OpenAPI réellement servi dans target/openapi.json. La CI le
 * compare à docs/api/openapi.json (openapi-diff) : une modification
 * incompatible pour les clients existants fait échouer le build.
 * Après une évolution voulue : cp bibliotheque-backend/target/openapi.json docs/api/
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
class OpenApiContractTest {

    static final Path CONTRAT_GENERE = Paths.get("target", "openapi.json");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void contratOpenApi_estGenerePourLaComparaisonEnCi() throws Exception {
        String brut = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        JsonNode contrat = objectMapper.readTree(brut);
        assertThat(contrat.path("paths").has("/api/v1/loans")).isTrue();
        assertThat(contrat.path("paths").has("/api/v1/profile")).isTrue();
        // Les anciens chemins restent servis (ApiVersionFilter) mais ne sont plus publiés.
        assertThat(contrat.path("paths").has("/borrow")).isFalse();

        Files.createDirectories(CONTRAT_GENERE.getParent());
        Files.write(CONTRAT_GENERE, objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(contrat).getBytes(StandardCharsets.UTF_8));
    }
}
