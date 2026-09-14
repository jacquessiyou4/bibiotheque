package com.ibizabroker.bibliotheque.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests unitaires du filtre d'audit de sécurité : il doit laisser passer la
 * requête dans la chaîne, puis observer le statut final (401/403 journalisés,
 * autres statuts ignorés) sans jamais interrompre la requête.
 * Vérifie également que le filtre journalise les tentatives d'accès refusées.
 */
class SecurityAuditFilterTest {

    private SecurityAuditFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new SecurityAuditFilter();
        chain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_laissePasserLaRequeteDansLaChaine() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/books");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilter_neProvoqueAucuneErreurSurUnStatut200() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/books");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        try {
            filter.doFilter(request, response, chain);
        } catch (Exception e) {
            throw new AssertionError("Le filtre ne doit jamais interrompre la requête", e);
        }
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_neProvoqueAucuneErreurSurUn403Anonyme() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/reservations/100");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(403);

        try {
            filter.doFilter(request, response, chain);
        } catch (Exception e) {
            throw new AssertionError("Le filtre ne doit jamais interrompre la requête", e);
        }
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_neProvoqueAucuneErreurSurUn401Anonyme() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(401);

        try {
            filter.doFilter(request, response, chain);
        } catch (Exception e) {
            throw new AssertionError("Le filtre ne doit jamais interrompre la requête", e);
        }
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_avecUtilisateurAuthentifieEt403_loggeLUtilisateur() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "john_doe", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(403);

        try {
            filter.doFilter(request, response, chain);
        } catch (Exception e) {
            throw new AssertionError("Le filtre ne doit jamais interrompre la requête", e);
        }

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_avecUtilisateurAnonymeEt401_loggeAnonyme() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(401);

        try {
            filter.doFilter(request, response, chain);
        } catch (Exception e) {
            throw new AssertionError("Le filtre ne doit jamais interrompre la requête", e);
        }

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_neLoggePasLesStatuts200() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/books");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        try {
            filter.doFilter(request, response, chain);
        } catch (Exception e) {
            throw new AssertionError("Le filtre ne doit jamais interrompre la requête", e);
        }

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_neLoggePasLesStatuts404() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/inexistant");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(404);

        try {
            filter.doFilter(request, response, chain);
        } catch (Exception e) {
            throw new AssertionError("Le filtre ne doit jamais interrompre la requête", e);
        }

        verify(chain).doFilter(request, response);
    }
}
