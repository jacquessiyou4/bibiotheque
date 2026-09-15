# 0003 — Monolithe découpé par fonctionnalité, sans modules Maven

- **Statut** : Acceptée
- **Date** : 15 septembre 2026

## Contexte

Le backend est rangé par couche technique (`controller`, `service`, `dao`…) :
indice de modularisation estimé à 0,33 (« monolithe en couches »). Des services
lisaient directement les repositories d'autres fonctionnalités, et rien
n'empêchait la structure de se dégrader. Le frontend déclarait ses 20
composants dans un seul `AppModule`, avec des cycles entre fonctionnalités.

## Décision

- Cible ≈ 0,55 : **un seul build**, découpé en packages par fonctionnalité
  (`catalogue`, `emprunts`, `reservations`, `utilisateurs`, `auth`, `shared`).
- Une fonctionnalité en appelle une autre par son service ou son interface
  d'API, jamais par ses repositories ni ses entités.
- Les règles sont **vérifiées par les tests** : ArchUnit côté backend
  (couches, cycles, entités hors de l'API, violations existantes gelées) et
  dependency-cruiser côté frontend.
- Pas de modules Maven, pas de JPMS, pas d'événements asynchrones entre
  fonctionnalités, pas de microservices.

## Conséquences

- Le découpage coûte peu : déplacements de classes, visibilité des packages,
  règles de test.
- La frontière n'est pas vérifiée par le compilateur entre fonctionnalités d'un
  même module. Les tests d'architecture font office de garde-fou.
- À réexaminer au-delà de 4 développeurs, ou si une fonctionnalité doit être
  livrée séparément (voir la section « Not now » du plan de modularisation).

## Mise en œuvre (15 septembre 2026)

- **Backend** : packages `catalogue`, `emprunts`, `reservations`, `utilisateurs`,
  `auth`, `donnees` (chacun en `api` / `web` / `internal`) et `shared`. Les
  échanges passent par `CatalogueApi`, `UtilisateursApi`, `EmpruntsApi` et
  `ReservationsApi`. Une réservation ne référence plus les entités `Books` et
  `Users` mais leurs identifiants ; les noms sont chargés par lot (deux requêtes
  par liste).
- **Frontend** : `core` (services, accès API, coquille), `shared` (pipe, modèles)
  et cinq modules de fonctionnalité chargés à la demande, sans changer les URL.
- **Vérification** : `ArchitectureTest` (plus aucune violation gelée) et
  `.dependency-cruiser.js` échouent en CI si une fonctionnalité utilise
  l'intérieur d'une autre ou si un cycle apparaît.
