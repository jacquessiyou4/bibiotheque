# --- Étape 1 : build --------------------------------------------------
# Angular 14 n'est officiellement supporté que jusqu'à Node 18
# ("Unsupported" sur Node 22+, voir README section 2). On construit donc
# avec une image Node 18, sans toucher à package.json.
FROM node:18-alpine AS build
WORKDIR /app

# Copié séparément pour profiter du cache Docker : si seul le code source
# change, npm ci ne se refait pas.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build -- --configuration production

# --- Étape 2 : exécution ------------------------------------------------
FROM nginx:1.27-alpine

COPY --from=build /app/dist/bibliotheque-frontend /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Script lancé automatiquement par l'image nginx officielle au démarrage :
# régénère assets/env.js à partir de la variable d'environnement API_URL.
COPY docker-entrypoint.d/40-env-config.sh /docker-entrypoint.d/40-env-config.sh
RUN chmod +x /docker-entrypoint.d/40-env-config.sh

HEALTHCHECK --interval=10s --timeout=5s --retries=5 \
  CMD wget -q --spider http://localhost:80 || exit 1

EXPOSE 80
