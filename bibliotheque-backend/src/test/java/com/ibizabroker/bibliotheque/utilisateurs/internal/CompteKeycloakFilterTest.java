package com.ibizabroker.bibliotheque.utilisateurs.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import javax.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompteKeycloakFilterTest {

    private static final String SUB_A1 = "5b0c7a1e-0000-4000-8000-00000000a001";

    private UsersRepository usersRepository;
    private FilterChain chain;
    private CompteKeycloakFilter filter;

    @BeforeEach
    void setUp() {
        usersRepository = mock(UsersRepository.class);
        chain = mock(FilterChain.class);
        filter = new CompteKeycloakFilter(usersRepository, new ObjectMapper());
        when(usersRepository.findByKeycloakSub(anyString())).thenReturn(Optional.empty());
        when(usersRepository.findByUsername(anyString())).thenReturn(Optional.empty());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sansJetonKeycloak_laissePasserSansInterrogerLaBase() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("a1", null));

        MockHttpServletResponse response = filtrer();

        verify(chain).doFilter(any(), any());
        verify(usersRepository, never()).findByUsername(anyString());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void premiereRequete_relieLeCompteLocalAuSubDuJeton() throws Exception {
        when(usersRepository.findByUsername("a1")).thenReturn(Optional.of(compte(2, "a1", null)));
        connecter(SUB_A1, "A1");

        filtrer();

        verify(usersRepository).lierCompteKeycloak(2, SUB_A1);
        verify(chain).doFilter(any(), any());
    }

    @Test
    void lienDejaVerifie_neReinterrogePasLaBase() throws Exception {
        when(usersRepository.findByKeycloakSub(SUB_A1)).thenReturn(Optional.of(compte(2, "a1", SUB_A1)));
        connecter(SUB_A1, "a1");

        filtrer();
        filtrer();

        verify(usersRepository, times(1)).findByKeycloakSub(SUB_A1);
        verify(chain, times(2)).doFilter(any(), any());
    }

    @Test
    void usernameRenommeDansKeycloak_leCompteLocalSuit() throws Exception {
        when(usersRepository.findByKeycloakSub(SUB_A1)).thenReturn(Optional.of(compte(2, "a1", SUB_A1)));
        connecter(SUB_A1, "adherent.un");

        filtrer();

        verify(usersRepository).renommer(2, "adherent.un");
        verify(chain).doFilter(any(), any());
    }

    @Test
    void renommageVersUnUsernameDejaPris_renvoie403() throws Exception {
        when(usersRepository.findByKeycloakSub(SUB_A1)).thenReturn(Optional.of(compte(2, "a1", SUB_A1)));
        when(usersRepository.findByUsername("a2")).thenReturn(Optional.of(compte(3, "a2", null)));
        connecter(SUB_A1, "a2");

        MockHttpServletResponse response = filtrer();

        assertThat(response.getStatus()).isEqualTo(403);
        verify(usersRepository, never()).renommer(anyInt(), anyString());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void usernameReattribueAUnAutreCompteKeycloak_renvoie403SansAccesAuxDonnees() throws Exception {
        when(usersRepository.findByUsername("a1")).thenReturn(Optional.of(compte(2, "a1", "ancien-sub")));
        connecter("nouveau-sub", "a1");

        MockHttpServletResponse response = filtrer();

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getContentAsString(StandardCharsets.UTF_8)).contains("\"code\":\"ACCOUNT_LINK_MISMATCH\"");
        verify(usersRepository, never()).lierCompteKeycloak(anyInt(), anyString());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void sansCompteLocal_laissePasserLesServicesRepondre() throws Exception {
        connecter("sub-inconnu", "ghost");

        filtrer();

        verify(chain).doFilter(any(), any());
        verify(usersRepository, never()).lierCompteKeycloak(anyInt(), anyString());
    }

    private MockHttpServletResponse filtrer() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/profile"), response, chain);
        return response;
    }

    private void connecter(String sub, String username) {
        Jwt jwt = Jwt.withTokenValue("jeton")
                .header("alg", "none")
                .subject(sub)
                .claim("preferred_username", username)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, Collections.emptyList(), username));
    }

    private Users compte(int id, String username, String sub) {
        Users user = new Users();
        user.setUserId(id);
        user.setUsername(username);
        user.setKeycloakSub(sub);
        return user;
    }
}
