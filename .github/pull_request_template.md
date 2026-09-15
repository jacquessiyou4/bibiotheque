## Ce que fait cette PR

<!-- Le problème résolu et la solution retenue, en quelques phrases. -->

## Comment tester

<!-- Étapes pour vérifier le changement : commandes, écrans, comptes de démonstration. -->

## Vérifications

- [ ] Tests ajoutés ou mis à jour (backend `mvn verify`, frontend `ng test`)
- [ ] Migration Flyway **nouvelle** (jamais de modification d'une migration déjà appliquée)
- [ ] Contrat d'API : `docs/api/openapi.json` mis à jour si l'API change
- [ ] Données personnelles : [docs/donnees-personnelles.md](../docs/donnees-personnelles.md) à jour si une donnée est ajoutée
- [ ] Décision d'architecture notable : ADR ajouté dans `docs/adr/`
- [ ] Exploitation : [docs/RUNBOOK.md](../docs/RUNBOOK.md) à jour si le déploiement change

## Reste à faire

<!-- Ce qui n'est volontairement pas couvert par cette PR. -->
