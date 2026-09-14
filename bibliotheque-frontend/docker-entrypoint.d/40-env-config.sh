#!/bin/sh
# Exécuté automatiquement par l'image nginx officielle au démarrage du
# conteneur (tout script exécutable dans /docker-entrypoint.d/ est lancé
# avant nginx). Objectif : injecter l'URL du backend ET l'URL de Keycloak
# SANS rebuild de l'image Angular, puisque c'est le NAVIGATEUR (pas le
# conteneur) qui exécutera ce code et qui doit savoir où joindre ces services.
set -e

API_URL="${API_URL:-http://localhost:8080}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8081}"
KEYCLOAK_REALM="${KEYCLOAK_REALM:-bibliotheque}"

cat > /usr/share/nginx/html/assets/env.js <<EOF
window.__env = {
  apiUrl: "${API_URL}",
  keycloakUrl: "${KEYCLOAK_URL}",
  keycloakRealm: "${KEYCLOAK_REALM}"
};
EOF

echo "[env-config] assets/env.js généré avec apiUrl=${API_URL}, keycloakUrl=${KEYCLOAK_URL}, keycloakRealm=${KEYCLOAK_REALM}"
