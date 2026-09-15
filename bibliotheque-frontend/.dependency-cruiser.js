/**
 * Règles d'architecture du frontend, vérifiées en CI par « npm run arch ».
 *
 *   core/      services transverses et accès à l'API, coquille de l'application
 *   shared/    pipe, modèles et types réutilisables (le pipe s'appuie sur core/services)
 *   features/  une fonctionnalité par dossier, chargée à la demande
 *
 * Voir docs/adr/0003-cible-modularisation.md.
 */
module.exports = {
  forbidden: [
    {
      name: 'no-circular',
      severity: 'error',
      comment: 'Un cycle d’imports empêche de découper l’application en modules chargés à la demande.',
      from: {},
      to: { circular: true }
    },
    {
      name: 'fonctionnalites-independantes',
      severity: 'error',
      comment: 'Une fonctionnalité n’importe jamais une autre : ce dont elles ont besoin toutes les deux va dans core ou shared.',
      from: { path: '^src/app/features/([^/]+)/' },
      to: { path: '^src/app/features/', pathNot: '^src/app/features/$1/' }
    },
    {
      name: 'core-et-shared-sans-fonctionnalite',
      severity: 'error',
      comment: 'core et shared sont chargés au démarrage : ils ne tirent aucune fonctionnalité dans le bundle initial.',
      from: { path: '^src/app/(core|shared)/' },
      to: { path: '^src/app/features/' }
    },
    {
      name: 'services-sans-composants',
      severity: 'error',
      comment: 'Services, modèles et utilitaires ne dépendent jamais d’un écran.',
      from: { path: '^src/app/(core/(services|api|auth|i18n|routing)|shared)/' },
      to: { path: '\\.component\\.ts$' }
    },
    {
      name: 'pas-de-spec-dans-le-code',
      severity: 'error',
      comment: 'Le code livré n’importe jamais un fichier de test.',
      from: { pathNot: '\\.spec\\.ts$' },
      to: { path: '\\.spec\\.ts$' }
    },
    {
      name: 'imports-resolus',
      severity: 'error',
      comment: 'Import vers un fichier ou un paquet introuvable.',
      from: {},
      to: { couldNotResolve: true }
    }
  ],
  options: {
    doNotFollow: { path: 'node_modules' },
    tsConfig: { fileName: 'tsconfig.json' },
    tsPreCompilationDeps: true,
    exclude: { path: '(^|/)(environments|test\\.ts$)' }
  }
};
