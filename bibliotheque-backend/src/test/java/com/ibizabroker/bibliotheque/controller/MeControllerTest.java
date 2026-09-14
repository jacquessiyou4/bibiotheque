package com.ibizabroker.bibliotheque.controller;

import com.ibizabroker.bibliotheque.dto.ProfileResponse;
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
 * Tests unitaires de MeController (endpoint obsolète) : il délègue au même
 * ProfileService que GET /profile.
 */
@ExtendWith(MockitoExtension.class)
class MeControllerTest {

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private MeController controller;

    @Test
    void me_renvoieLeProfilDeLUtilisateurConnecte() {
        Authentication a1 = new TestingAuthenticationToken("a1", null, "ROLE_ADHERENT");
        ProfileResponse profil = new ProfileResponse(2, "a1", "Adhérent Un", null, Collections.singletonList("ADHERENT"));
        when(profileService.profilCourant(a1)).thenReturn(profil);

        assertThat(controller.me(a1)).isSameAs(profil);
    }
}
