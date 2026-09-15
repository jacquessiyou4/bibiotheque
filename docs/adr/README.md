# Décisions d'architecture (ADR)

Chaque fichier consigne une décision : son contexte, ce qui a été choisi, ce
que l'on accepte en échange. Une décision n'est jamais réécrite : si elle
change, on ajoute un nouvel ADR qui la remplace et on marque l'ancien
« Remplacé par ».

| N° | Décision | Statut |
|---|---|---|
| [0001](0001-keycloak-fournisseur-identite.md) | Keycloak comme fournisseur d'identité | Acceptée |
| [0002](0002-schema-partage.md) | Une seule base, schéma partagé entre fonctionnalités | Acceptée |
| [0003](0003-cible-modularisation.md) | Monolithe découpé par fonctionnalité, sans modules Maven | Acceptée |
| [0004](0004-glossaire-et-nommage.md) | Glossaire et règles de nommage | Acceptée |

Modèle : copier un fichier existant, numéroter à la suite.
