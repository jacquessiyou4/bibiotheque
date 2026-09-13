# NB : le contexte de build est la RACINE du projet (voir docker-compose.yml,
# build.context: .), pour que le Dockerfile puisse lire à la fois
# bibliotheque-backend/ et bibliotheque-frontend/.

# --- Étape 1bis : build frontend -----------------------------------------
# Angular 14 n'est officiellement supporté que jusqu'à Node 18
# ("Unsupported" sur Node 22+, voir README section 2).
# Depuis la fusion, le build Angular est EMBARQUÉ dans le jar Spring Boot :
# une seule application, un seul port (8080), plus de conteneur nginx.
# NB : cette étape est déclarée AVANT l'étape maven (qui en dépend via
# COPY --from) : avec une référence en avant, BuildKit (v0.32) détecte une
# fausse "circular dependency".
FROM node:18-alpine AS frontend-build
WORKDIR /frontend
COPY bibliotheque-frontend/package.json bibliotheque-frontend/package-lock.json ./
RUN npm ci
COPY bibliotheque-frontend/ ./
RUN npm run build -- --configuration production

# --- Étape 1 : build backend ---------------------------------------------
# Le pom.xml cible Java 1.8 et embarque une version de Lombok trop ancienne
# pour l'API interne du compilateur des JDK 17+ (voir README section 2 :
# NoSuchFieldError sur JCTree$JCImport). Plutôt que de moderniser le
# projet — interdit ici, on ne touche pas au pom.xml pour cette raison —
# on construit avec un JDK 8, qui correspond à ce que le projet attend.
FROM maven:3.8.6-eclipse-temurin-8 AS build
WORKDIR /app
COPY bibliotheque-backend/pom.xml .
RUN mvn -B dependency:go-offline

# Le build Angular est copié dans les resources du backend ; il est alors
# empaqueté tel quel dans le jar et servi automatiquement par Spring Boot
# depuis /static (voir SpaConfiguration pour le routage client).
COPY --from=frontend-build /frontend/dist/bibliotheque-frontend ./src/main/resources/static

COPY bibliotheque-backend/src ./src
RUN mvn -B clean package -DskipTests

# --- Étape 2 : exécution --------------------------------------------------
FROM eclipse-temurin:8-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
