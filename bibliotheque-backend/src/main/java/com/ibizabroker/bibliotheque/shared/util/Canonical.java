package com.ibizabroker.bibliotheque.shared.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Forme canonique des saisies, appliquée dès la réception (setters des DTO) :
 * « Marie ␠» et « marie » désignent le même compte, « Victor  Hugo » et
 * « Victor Hugo » le même auteur. Sans cela, V3 a dû corriger après coup des
 * usernames que Keycloak stocke en minuscules.
 */
public final class Canonical {

    private static final Pattern ESPACES = Pattern.compile("\\s+");

    private Canonical() {
    }

    /** Texte libre : Unicode NFC, espaces fusionnés, bords rognés. null reste null. */
    public static String texte(String brut) {
        if (brut == null) {
            return null;
        }
        String normalise = Normalizer.normalize(brut, Normalizer.Form.NFC);
        return ESPACES.matcher(normalise).replaceAll(" ").trim();
    }

    /** Username : comme texte(), puis en minuscules (règle de Keycloak). */
    public static String username(String brut) {
        String texte = texte(brut);
        return texte == null ? null : texte.toLowerCase(Locale.ROOT);
    }
}
