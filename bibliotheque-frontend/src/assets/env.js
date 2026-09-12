// Valeur par défaut utilisée par `ng serve` (développement local, hors Docker).
// En conteneur, ce fichier est régénéré au démarrage de nginx à partir des
// variables d'environnement (voir docker-entrypoint.d/40-env-config.sh).
window.__env = {
  apiUrl: "http://localhost:8080",
  keycloakUrl: "http://localhost:8081",
  keycloakRealm: "bibliotheque"
};
