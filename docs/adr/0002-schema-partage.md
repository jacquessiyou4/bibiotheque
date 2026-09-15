# 0002 — Une seule base, schéma partagé entre fonctionnalités

- **Statut** : Acceptée
- **Date** : 15 septembre 2026

## Contexte

Catalogue, emprunts, réservations et utilisateurs partagent la base
PostgreSQL `bibliotheque`. Les écrans de liste ont besoin de recouper ces
données (nom du livre, nom de l'emprunteur). La grille de décision sur
l'isolation des données recommande, pour une équipe de 1 à 3 personnes, le
niveau 0 (tables partagées) ou 1 (préfixes), et déconseille les niveaux
supérieurs.

## Décision

- Niveau d'isolation **0** : une base, un schéma, des migrations Flyway
  communes et numérotées (`V1`, `V2`…).
- L'intégrité est garantie **par la base** : clés étrangères entre
  fonctionnalités (`borrow`, `reservation` vers `books` et `users`), unicité
  (`lower(username)`, réservation active unique), contrôles (`no_of_copies >= 0`).
- Dans le code, une fonctionnalité ne lit pas les tables d'une autre par ses
  repositories : elle passe par le service de l'autre fonctionnalité (voir ADR 0003).

## Conséquences

- Pas de transactions distribuées ni de synchronisation entre bases.
- Clés étrangères entre fonctionnalités : un anti-pattern (DV-02) accepté
  sciemment, parce que la cohérence des emprunts vaut plus ici que
  l'indépendance du déploiement.
- Supprimer un livre encore emprunté est refusé par la base (réponse `409`,
  code `DATA_CONFLICT`).
- À réexaminer si une fonctionnalité doit être déployée ou mise à l'échelle
  seule, ou changer de moteur de stockage.
