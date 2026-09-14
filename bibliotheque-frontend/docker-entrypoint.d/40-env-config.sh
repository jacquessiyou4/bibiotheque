#!/bin/sh
# Exécuté automatiquement par l'image nginx officielle au démarrage du
# conteneur (tout script exécutable dans /docker-entrypoint.d/ est lancé
# avant nginx). Objectif : injecter l'URL du backend SANS rebuild de l'image
# Angular, puisque c'est le NAVIGATEUR (pas le conteneur) qui exécutera ce
# code et qui doit savoir où joindre le backend. Le navigateur ne contacte
# plus Keycloak : la connexion passe par le backend (POST /auth/token).
set -e

API_URL="${API_URL:-http://localhost:8080}"

cat > /usr/share/nginx/html/assets/env.js <<EOF
window.__env = {
  apiUrl: "${API_URL}"
};
EOF

echo "[env-config] assets/env.js généré avec apiUrl=${API_URL}"
