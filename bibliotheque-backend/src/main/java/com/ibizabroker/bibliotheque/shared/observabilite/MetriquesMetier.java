package com.ibizabroker.bibliotheque.shared.observabilite;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Compteurs métier exposés sur /actuator/prometheus et suivis dans Grafana
 * (ops/observability). Ils sont enregistrés dès le démarrage : un tableau de
 * bord affiche 0 plutôt qu'une courbe absente tant que rien n'est arrivé.
 *
 * <pre>
 * bibliotheque_emprunts_total                 emprunts enregistrés
 * bibliotheque_retours_total                  livres rendus
 * bibliotheque_reservations_total             réservations créées
 * bibliotheque_reservations_expirees_total    réservations passées à EXPIREE
 * bibliotheque_connexions_total{resultat}     connexions par POST /api/v1/auth/token (succes, echec)
 * </pre>
 */
@Component
public class MetriquesMetier {

    private final Counter emprunts;
    private final Counter retours;
    private final Counter reservations;
    private final Counter reservationsExpirees;
    private final Counter connexionsReussies;
    private final Counter connexionsEchouees;

    public MetriquesMetier(MeterRegistry registry) {
        this.emprunts = Counter.builder("bibliotheque.emprunts")
                .description("Emprunts enregistrés").register(registry);
        this.retours = Counter.builder("bibliotheque.retours")
                .description("Livres rendus").register(registry);
        this.reservations = Counter.builder("bibliotheque.reservations")
                .description("Réservations créées").register(registry);
        this.reservationsExpirees = Counter.builder("bibliotheque.reservations.expirees")
                .description("Réservations expirées faute d'emprunt").register(registry);
        this.connexionsReussies = Counter.builder("bibliotheque.connexions")
                .description("Connexions par identifiant et mot de passe").tag("resultat", "succes").register(registry);
        this.connexionsEchouees = Counter.builder("bibliotheque.connexions")
                .description("Connexions par identifiant et mot de passe").tag("resultat", "echec").register(registry);
    }

    public void empruntEnregistre() {
        emprunts.increment();
    }

    public void retourEnregistre() {
        retours.increment();
    }

    public void reservationCreee() {
        reservations.increment();
    }

    public void reservationsExpirees(int nombre) {
        if (nombre > 0) {
            reservationsExpirees.increment(nombre);
        }
    }

    public void connexion(boolean reussie) {
        (reussie ? connexionsReussies : connexionsEchouees).increment();
    }
}
