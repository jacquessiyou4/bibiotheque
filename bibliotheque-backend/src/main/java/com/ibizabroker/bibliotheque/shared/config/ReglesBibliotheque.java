package com.ibizabroker.bibliotheque.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * Règles de prêt de la bibliothèque, réglables sans recompiler
 * (application.properties ou variables APP_BIBLIOTHEQUE_*). Une valeur hors
 * bornes empêche le démarrage au lieu de produire des dates absurdes.
 */
@Component
@Validated
@ConfigurationProperties(prefix = "app.bibliotheque")
public class ReglesBibliotheque {

    /** Durée d'un emprunt avant la date limite de retour. */
    @Min(1)
    @Max(365)
    private int dureeEmpruntJours = 7;

    /** Durée pendant laquelle une réservation reste active. */
    @Min(1)
    @Max(365)
    private int dureeReservationJours = 7;

    /** Réservations actives (en attente ou disponibles) autorisées par adhérent. */
    @Min(1)
    @Max(100)
    private int reservationsActivesMax = 3;

    public int getDureeEmpruntJours() { return dureeEmpruntJours; }
    public void setDureeEmpruntJours(int dureeEmpruntJours) { this.dureeEmpruntJours = dureeEmpruntJours; }
    public int getDureeReservationJours() { return dureeReservationJours; }
    public void setDureeReservationJours(int dureeReservationJours) { this.dureeReservationJours = dureeReservationJours; }
    public int getReservationsActivesMax() { return reservationsActivesMax; }
    public void setReservationsActivesMax(int reservationsActivesMax) { this.reservationsActivesMax = reservationsActivesMax; }
}
