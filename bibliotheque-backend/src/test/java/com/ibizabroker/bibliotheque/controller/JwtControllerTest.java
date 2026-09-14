package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dto.ProfileResponse;
import com.ibizabroker.bibliotheque.entity.JwtRequest;
import com.ibizabroker.bibliotheque.entity.JwtResponse;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.service.JwtService;
import com.ibizabroker.bibliotheque.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de JwtController : JwtService et ProfileService simulés,
 * aucun contexte Spring. Les accès HTTP (401 sans jeton, 404) sont couverts
 * par JwtControllerIntegrationTest.
 */
@ExtendWith(MockitoExtension.class)
class JwtControllerTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private JwtController controller;

    @Test
    void createJwtToken_delegueAuServiceLegacy() throws Exception {
        JwtRequest demande = new JwtRequest();
        demande.setUsername("A1");
        demande.setPassword("mot-de-passe");
        JwtResponse reponse = new JwtResponse(new Users(), "jeton-local");
        when(jwtService.createJwtToken(demande)).thenReturn(reponse);

        assertThat(controller.createJwtToken(demande)).isSameAs(reponse);
    }

    @Test
    void profile_renvoieLeProfilDuJeton() {
        Authentication a1 = new TestingAuthenticationToken("a1", null, "ROLE_ADHERENT");
        ProfileResponse profil = new ProfileResponse(2, "a1", "Adhérent Un", "a1@bibliotheque.local",
                Collections.singletonList("ADHERENT"));
        when(profileService.profilCourant(a1)).thenReturn(profil);

        assertThat(controller.profile(a1)).isSameAs(profil);
    }
}
