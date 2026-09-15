package com.ibizabroker.bibliotheque.utilisateurs.internal;

import com.ibizabroker.bibliotheque.shared.error.NotFoundException;
import com.ibizabroker.bibliotheque.utilisateurs.api.ProfileResponse;
import com.ibizabroker.bibliotheque.utilisateurs.api.UserResponse;
import com.ibizabroker.bibliotheque.utilisateurs.api.UtilisateurResume;
import com.ibizabroker.bibliotheque.utilisateurs.api.UtilisateursApi;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Implémentation de UtilisateursApi, seule porte d'entrée vers les comptes pour les autres fonctionnalités. */
@Service
public class UsersService implements UtilisateursApi {

    static final String NOM_ANONYME = "Utilisateur anonymisé";

    private final UsersRepository usersRepository;
    private final ProfileService profileService;

    public UsersService(UsersRepository usersRepository, ProfileService profileService) {
        this.usersRepository = usersRepository;
        this.profileService = profileService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UtilisateurResume> utilisateur(Integer userId) {
        return usersRepository.findById(userId).map(UsersService::resume);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UtilisateurResume> parUsername(String username) {
        return usersRepository.findByUsername(username).map(UsersService::resume);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, UtilisateurResume> utilisateurs(Collection<Integer> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return usersRepository.findAllById(userIds).stream()
                .map(UsersService::resume)
                .collect(Collectors.toMap(UtilisateurResume::getUserId, Function.identity()));
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse profilCourant(Authentication authentication) {
        return profileService.profilCourant(authentication);
    }

    /**
     * Anonymise un compte local (droit à l'effacement). Emprunts et réservations
     * restent attachés au compte, qui n'identifie plus personne : le stock et
     * les statistiques de prêt restent justes.
     */
    @Override
    @Transactional
    public UserResponse anonymiser(Integer userId) {
        Users compte = usersRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND",
                        "Utilisateur avec id " + userId + " introuvable."));
        compte.setUsername("anonyme-" + userId);
        compte.setName(NOM_ANONYME);
        compte.setPassword(null);
        compte.setKeycloakSub(null);
        compte.setRole(new HashSet<>());
        Users anonyme = usersRepository.save(compte);
        return new UserResponse(anonyme.getUserId(), anonyme.getUsername(), anonyme.getName(), Collections.emptyList());
    }

    private static UtilisateurResume resume(Users user) {
        return new UtilisateurResume(user.getUserId(), user.getUsername(), user.getName());
    }
}
