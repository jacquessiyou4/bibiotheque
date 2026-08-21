# syntax=docker/dockerfile:1
# ---- Build stage ----
FROM maven:3.8-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Réchauffe le cache .m2 séparément : un échec réseau sur les sources ne perd
# pas les dépendances déjà téléchargées, et les rebuilds suivants sont rapides.
RUN --mount=type=cache,target=/root/.m2 mvn -B dependency:go-offline || true
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

# ---- Runtime stage ----
# Spring Boot 2.7.18 est compatible Java 17.
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
