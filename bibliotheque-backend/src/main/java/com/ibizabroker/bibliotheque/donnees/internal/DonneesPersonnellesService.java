package com.ibizabroker.bibliotheque.donnees.internal;

import com.ibizabroker.bibliotheque.donnees.web.DonneesPersonnellesResponse;
import com.ibizabroker.bibliotheque.emprunts.api.EmpruntsApi;
import com.ibizabroker.bibliotheque.reservations.api.ReservationsApi;
import com.ibizabroker.bibliotheque.utilisateurs.api.ProfileResponse;
import com.ibizabroker.bibliotheque.utilisateurs.api.UserResponse;
import com.ibizabroker.bibliotheque.utilisateurs.api.UtilisateursApi;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Droits RGPD : export des données d'un adhérent et anonymisation d'un compte.
 * Fonctionnalité à part entière, qui ne connaît les autres que par leurs API.
 * Voir docs/donnees-personnelles.md.
 */
@Service
public class DonneesPersonnellesService {

    private final UtilisateursApi utilisateursApi;
    private final EmpruntsApi empruntsApi;
    private final ReservationsApi reservationsApi;

    public DonneesPersonnellesService(UtilisateursApi utilisateursApi, EmpruntsApi empruntsApi,
                                      ReservationsApi reservationsApi) {
        this.utilisateursApi = utilisateursApi;
        this.empruntsApi = empruntsApi;
        this.reservationsApi = reservationsApi;
    }

    /** Toutes les données de l'utilisateur du jeton, jamais celles d'un autre. */
    @Transactional(readOnly = true)
    public DonneesPersonnellesResponse exporter(Authentication authentication) {
        ProfileResponse profil = utilisateursApi.profilCourant(authentication);
        return new DonneesPersonnellesResponse(profil,
                empruntsApi.empruntsDe(profil.getUserId()),
                reservationsApi.reservationsDe(profil.getUserId()),
                Instant.now());
    }

    /** Droit à l'effacement : voir UtilisateursApi#anonymiser. */
    @Transactional
    public UserResponse anonymiser(Integer userId) {
        return utilisateursApi.anonymiser(userId);
    }
}
