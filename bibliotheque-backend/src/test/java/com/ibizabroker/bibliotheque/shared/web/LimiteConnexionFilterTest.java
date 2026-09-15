package com.ibizabroker.bibliotheque.shared.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class LimiteConnexionFilterTest {

    private MutableClock horloge;
    private FilterChain chain;
    private LimiteConnexionFilter filter;

    @BeforeEach
    void setUp() {
        horloge = new MutableClock(Instant.parse("2026-09-15T10:00:00Z"));
        chain = mock(FilterChain.class);
        filter = new LimiteConnexionFilter(new ObjectMapper(), horloge);
    }

    @Test
    void jusquALaLimite_lesTentativesPassent() throws Exception {
        for (int i = 0; i < LimiteConnexionFilter.MAX_TENTATIVES; i++) {
            assertThat(connexion("10.0.0.1").getStatus()).isEqualTo(200);
        }

        verify(chain, times(LimiteConnexionFilter.MAX_TENTATIVES)).doFilter(any(), any());
    }

    @Test
    void auDelaDeLaLimite_renvoie429AvecRetryAfter() throws Exception {
        for (int i = 0; i < LimiteConnexionFilter.MAX_TENTATIVES; i++) {
            connexion("10.0.0.1");
        }
        horloge.avancer(Duration.ofSeconds(20));

        MockHttpServletResponse refus = connexion("10.0.0.1");

        assertThat(refus.getStatus()).isEqualTo(429);
        assertThat(refus.getHeader("Retry-After")).isEqualTo("40");
        assertThat(refus.getContentAsString(StandardCharsets.UTF_8)).contains("\"code\":\"TOO_MANY_LOGIN_ATTEMPTS\"");
        verify(chain, times(LimiteConnexionFilter.MAX_TENTATIVES)).doFilter(any(), any());
    }

    @Test
    void apresLaFenetre_lesTentativesSontDeNouveauAcceptees() throws Exception {
        for (int i = 0; i < LimiteConnexionFilter.MAX_TENTATIVES; i++) {
            connexion("10.0.0.1");
        }
        horloge.avancer(LimiteConnexionFilter.FENETRE.plusSeconds(1));

        assertThat(connexion("10.0.0.1").getStatus()).isEqualTo(200);
    }

    @Test
    void chaqueAdresseASaPropreLimite() throws Exception {
        for (int i = 0; i < LimiteConnexionFilter.MAX_TENTATIVES; i++) {
            connexion("10.0.0.1");
        }

        assertThat(connexion("10.0.0.2").getStatus()).isEqualTo(200);
    }

    @Test
    void autresRequetes_neSontJamaisLimitees() throws Exception {
        for (int i = 0; i < LimiteConnexionFilter.MAX_TENTATIVES + 5; i++) {
            MockHttpServletRequest requete = new MockHttpServletRequest("GET", "/profile");
            requete.setRemoteAddr("10.0.0.1");
            filter.doFilter(requete, new MockHttpServletResponse(), chain);
        }

        verify(chain, times(LimiteConnexionFilter.MAX_TENTATIVES + 5)).doFilter(any(), any());
    }

    private MockHttpServletResponse connexion(String ip) throws Exception {
        MockHttpServletRequest requete = new MockHttpServletRequest("POST", "/api/v1/auth/token");
        requete.setRemoteAddr(ip);
        MockHttpServletResponse reponse = new MockHttpServletResponse();
        filter.doFilter(requete, reponse, chain);
        return reponse;
    }

    /** Horloge de test que l'on fait avancer à la main. */
    static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void avancer(Duration duree) {
            instant = instant.plus(duree);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
