package com.ibizabroker.bibliotheque.shared.web;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ApiVersionFilterTest {

    private final ApiVersionFilter filter = new ApiVersionFilter();

    @Test
    void versionner_couvreChaqueAncienPrefixeSansConfondreLesChemins() {
        assertThat(ApiVersionFilter.versionner("/borrow")).isEqualTo("/api/v1/loans");
        assertThat(ApiVersionFilter.versionner("/borrow/user/3")).isEqualTo("/api/v1/loans/user/3");
        assertThat(ApiVersionFilter.versionner("/admin/books/7")).isEqualTo("/api/v1/books/7");
        assertThat(ApiVersionFilter.versionner("/admin/users/2/anonymisation")).isEqualTo("/api/v1/users/2/anonymisation");
        assertThat(ApiVersionFilter.versionner("/api/reservations/5/annuler")).isEqualTo("/api/v1/reservations/5/annuler");
        assertThat(ApiVersionFilter.versionner("/auth/token")).isEqualTo("/api/v1/auth/token");
        assertThat(ApiVersionFilter.versionner("/profile/export")).isEqualTo("/api/v1/profile/export");
        // Préfixe seulement sur une frontière de segment.
        assertThat(ApiVersionFilter.versionner("/profiles")).isNull();
        assertThat(ApiVersionFilter.versionner("/api/v1/loans")).isNull();
        assertThat(ApiVersionFilter.versionner("/actuator/health")).isNull();
    }

    @Test
    void ancienChemin_estRedirigeEnInterneEtAnnonceCommeObsolete() throws Exception {
        MockHttpServletRequest requete = new MockHttpServletRequest("GET", "/borrow/user/3");
        requete.setServerName("localhost");
        requete.setServerPort(8080);
        MockHttpServletResponse reponse = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(requete, reponse, chain);

        ArgumentCaptor<HttpServletRequest> transmise = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(chain).doFilter(transmise.capture(), any());
        assertThat(transmise.getValue().getRequestURI()).isEqualTo("/api/v1/loans/user/3");
        assertThat(transmise.getValue().getRequestURL().toString()).isEqualTo("http://localhost:8080/api/v1/loans/user/3");
        assertThat(transmise.getValue().getPathInfo()).isEqualTo("/api/v1/loans/user/3");
        assertThat(reponse.getHeader("Deprecation")).isEqualTo("true");
        assertThat(reponse.getHeader("Sunset")).isEqualTo(ApiVersionFilter.SUNSET);
        assertThat(reponse.getHeader("Link")).isEqualTo("</api/v1/loans/user/3>; rel=\"successor-version\"");
    }

    @Test
    void cheminServletTomcat_estRemplaceDansServletPath() throws Exception {
        MockHttpServletRequest requete = new MockHttpServletRequest("GET", "/profile");
        requete.setServletPath("/profile");
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(requete, new MockHttpServletResponse(), chain);

        ArgumentCaptor<HttpServletRequest> transmise = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(chain).doFilter(transmise.capture(), any());
        assertThat(transmise.getValue().getServletPath()).isEqualTo("/api/v1/profile");
        assertThat(transmise.getValue().getPathInfo()).isNull();
    }

    @Test
    void cheminVersionne_passeSansEnTeteDObsolescence() throws Exception {
        MockHttpServletRequest requete = new MockHttpServletRequest("GET", "/api/v1/loans");
        MockHttpServletResponse reponse = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(requete, reponse, chain);

        verify(chain).doFilter(requete, reponse);
        assertThat(reponse.getHeader("Deprecation")).isNull();
    }
}
