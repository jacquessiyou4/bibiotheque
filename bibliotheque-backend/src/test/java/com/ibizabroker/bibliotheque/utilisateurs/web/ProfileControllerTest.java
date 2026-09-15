package com.ibizabroker.bibliotheque.utilisateurs.web;

import com.ibizabroker.bibliotheque.utilisateurs.api.ProfileResponse;
import com.ibizabroker.bibliotheque.utilisateurs.internal.ProfileService;
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
 * Tests unitaires de ProfileController : ProfileService simulé, aucun
 * contexte Spring. Les accès HTTP (401 sans jeton, 404) sont couverts par
 * ProfileApiIntegrationTest.
 */
@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private ProfileController controller;

    @Test
    void profile_renvoieLeProfilDuJeton() {
        Authentication a1 = new TestingAuthenticationToken("a1", null, "ROLE_ADHERENT");
        ProfileResponse profil = new ProfileResponse(2, "a1", "Adhérent Un", "a1@bibliotheque.local",
                Collections.singletonList("ADHERENT"));
        when(profileService.profilCourant(a1)).thenReturn(profil);

        assertThat(controller.profile(a1)).isSameAs(profil);
    }
}
