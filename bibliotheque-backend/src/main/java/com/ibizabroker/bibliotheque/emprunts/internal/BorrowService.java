package com.ibizabroker.bibliotheque.emprunts.internal;

import com.ibizabroker.bibliotheque.catalogue.api.CatalogueApi;
import com.ibizabroker.bibliotheque.catalogue.api.LivreResume;
import com.ibizabroker.bibliotheque.emprunts.api.BorrowResponse;
import com.ibizabroker.bibliotheque.emprunts.api.EmpruntsApi;
import com.ibizabroker.bibliotheque.shared.config.ReglesBibliotheque;
import com.ibizabroker.bibliotheque.shared.error.BadRequestException;
import com.ibizabroker.bibliotheque.shared.error.ForbiddenException;
import com.ibizabroker.bibliotheque.shared.error.NotFoundException;
import com.ibizabroker.bibliotheque.shared.observabilite.MetriquesMetier;
import com.ibizabroker.bibliotheque.utilisateurs.api.UtilisateurResume;
import com.ibizabroker.bibliotheque.utilisateurs.api.UtilisateursApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Emprunts et retours. Le stock et les comptes appartiennent à d'autres
 * fonctionnalités : ils sont lus et modifiés par CatalogueApi et UtilisateursApi.
 */
@Service
@Slf4j
public class BorrowService implements EmpruntsApi {

    private final BorrowRepository borrowRepository;
    private final CatalogueApi catalogueApi;
    private final UtilisateursApi utilisateursApi;
    private final ReglesBibliotheque regles;
    private final MetriquesMetier metriques;

    public BorrowService(BorrowRepository borrowRepository, CatalogueApi catalogueApi, UtilisateursApi utilisateursApi,
                         ReglesBibliotheque regles, MetriquesMetier metriques) {
        this.borrowRepository = borrowRepository;
        this.catalogueApi = catalogueApi;
        this.utilisateursApi = utilisateursApi;
        this.regles = regles;
        this.metriques = metriques;
    }

    @Transactional
    public Borrow borrowBook(Integer bookId, Integer userId) {
        UtilisateurResume user = utilisateursApi.utilisateur(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "Utilisateur introuvable"));
        LivreResume livre = catalogueApi.livre(bookId)
                .orElseThrow(() -> new NotFoundException("BOOK_NOT_FOUND", "Livre introuvable"));

        // Contrôle et retrait en une requête : deux emprunts simultanés du
        // dernier exemplaire ne peuvent plus réussir tous les deux.
        if (!catalogueApi.retirerExemplaire(bookId)) {
            throw new BadRequestException("BOOK_UNAVAILABLE", "Le livre \"" + livre.getBookName() + "\" n'est plus disponible.");
        }

        LocalDateTime maintenant = LocalDateTime.now();
        Borrow borrow = new Borrow();
        borrow.setBookId(bookId);
        borrow.setUserId(userId);
        borrow.setIssueDate(maintenant);
        borrow.setDueDate(maintenant.plusDays(regles.getDureeEmpruntJours()));
        Borrow enregistre = borrowRepository.save(borrow);
        metriques.empruntEnregistre();

        log.info("Emprunt: {} a emprunté une copie de \"{}\"", user.getName(), livre.getBookName());
        return enregistre;
    }

    @Transactional(readOnly = true)
    public List<Borrow> findAll() {
        return borrowRepository.findAll();
    }

    /**
     * Retour d'un emprunt. Si proprietaireAttendu est renseigné (utilisateur
     * non bibliothécaire), l'emprunt doit lui appartenir, sinon 403.
     */
    @Transactional
    public Borrow returnBook(Integer borrowId, Integer proprietaireAttendu) {
        Borrow borrowBook = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new NotFoundException("BORROW_NOT_FOUND", "Emprunt introuvable"));
        if (proprietaireAttendu != null && !proprietaireAttendu.equals(borrowBook.getUserId())) {
            throw new ForbiddenException("BORROW_NOT_OWNED", "Accès refusé : cet emprunt ne vous appartient pas.");
        }
        // Sans ce contrôle, rendre deux fois le même emprunt ajoutait une
        // copie fantôme au stock.
        if (borrowBook.getReturnDate() != null) {
            throw new BadRequestException("BORROW_ALREADY_RETURNED", "Cet emprunt a déjà été rendu.");
        }
        LivreResume livre = catalogueApi.livre(borrowBook.getBookId())
                .orElseThrow(() -> new NotFoundException("BOOK_NOT_FOUND", "Livre introuvable"));

        catalogueApi.remettreExemplaire(livre.getBookId());

        borrowBook.setReturnDate(LocalDateTime.now());

        log.info("Retour: emprunt {} rendu pour \"{}\"", borrowBook.getBorrowId(), livre.getBookName());
        Borrow rendu = borrowRepository.save(borrowBook);
        metriques.retourEnregistre();
        return rendu;
    }

    /**
     * Identifiant de l'utilisateur local correspondant au compte Keycloak
     * authentifié (preferred_username).
     */
    public Integer resolveUserId(String username) {
        return utilisateursApi.parUsername(username)
                .map(UtilisateurResume::getUserId)
                .orElseThrow(() -> new ForbiddenException("LOCAL_ACCOUNT_MISSING",
                        "Aucun compte local pour le compte Keycloak « " + username + " »."));
    }

    @Transactional(readOnly = true)
    public List<Borrow> findByUserId(Integer userId) {
        return borrowRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Borrow> findByBookId(Integer bookId) {
        return borrowRepository.findByBookId(bookId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowResponse> empruntsDe(Integer userId) {
        return findByUserId(userId).stream().map(BorrowResponse::from).collect(Collectors.toList());
    }
}
