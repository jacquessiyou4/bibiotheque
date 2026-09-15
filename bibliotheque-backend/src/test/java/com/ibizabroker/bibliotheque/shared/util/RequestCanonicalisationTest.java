package com.ibizabroker.bibliotheque.shared.util;

import com.ibizabroker.bibliotheque.catalogue.web.BookRequest;
import com.ibizabroker.bibliotheque.utilisateurs.web.UserCreateRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les DTO appliquent la forme canonique dès la désérialisation JSON : le
 * contrôleur, le service et la base ne voient jamais « Marie ␠».
 */
class RequestCanonicalisationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void userCreateRequest_usernameEnMinusculesEtNomNettoye() throws Exception {
        UserCreateRequest demande = objectMapper.readValue(
                "{\"username\":\"  Marie \",\"name\":\"  Marie   Curie \"}", UserCreateRequest.class);

        assertThat(demande.getUsername()).isEqualTo("marie");
        assertThat(demande.getName()).isEqualTo("Marie Curie");
    }

    @Test
    void bookRequest_champsTexteNettoyesEtGenreVideEnNull() throws Exception {
        BookRequest demande = objectMapper.readValue(
                "{\"bookName\":\" Les   Misérables \",\"bookAuthor\":\"Victor  Hugo \",\"bookGenre\":\"  \",\"noOfCopies\":2}",
                BookRequest.class);

        assertThat(demande.getBookName()).isEqualTo("Les Misérables");
        assertThat(demande.getBookAuthor()).isEqualTo("Victor Hugo");
        assertThat(demande.getBookGenre()).isNull();
        assertThat(demande.getNoOfCopies()).isEqualTo(2);
    }

    @Test
    void bookRequest_constructeurAppliqueAussiLaFormeCanonique() {
        BookRequest demande = new BookRequest(" Titre ", " Auteur ", " Roman ", 1);

        assertThat(demande.getBookName()).isEqualTo("Titre");
        assertThat(demande.getBookAuthor()).isEqualTo("Auteur");
        assertThat(demande.getBookGenre()).isEqualTo("Roman");
    }
}
