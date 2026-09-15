package com.ibizabroker.bibliotheque.shared.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CanonicalTest {

    @Test
    void texte_rogneEtFusionneLesEspaces() {
        assertThat(Canonical.texte("  Victor \t  Hugo \n")).isEqualTo("Victor Hugo");
    }

    @Test
    void texte_normaliseLUnicodeEnNfc() {
        // « é » saisi comme « e » + accent combinant (macOS, copier-coller) :
        // même chaîne que le « é » précomposé une fois normalisé.
        assertThat(Canonical.texte("Misérables")).isEqualTo("Misérables");
    }

    @Test
    void username_passeEnMinusculesSansDependreDeLaLangue() {
        assertThat(Canonical.username("  Marie ")).isEqualTo("marie");
        // Locale.ROOT : « I » reste « i » même sur une JVM configurée en turc.
        assertThat(Canonical.username("ADMIN")).isEqualTo("admin");
    }

    @Test
    void null_resteNull() {
        assertThat(Canonical.texte(null)).isNull();
        assertThat(Canonical.username(null)).isNull();
    }
}
