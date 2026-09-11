# --- Étape 1 : build -----------------------------------------------------
# Le pom.xml cible Java 1.8 et embarque une version de Lombok trop ancienne
# pour l'API interne du compilateur des JDK 17+ (voir README section 2 :
# NoSuchFieldError sur JCTree$JCImport). Plutôt que de moderniser le
# projet — interdit ici, on ne touche pas au pom.xml pour cette raison —
# on construit avec un JDK 8, qui correspond à ce que le projet attend.
FROM maven:3.8.6-eclipse-temurin-8 AS build
WORKDIR /app

# Copié séparément pour profiter du cache Docker : les dépendances ne sont
# retéléchargées que si pom.xml change, pas à chaque modification de code.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Étape 2 : exécution --------------------------------------------------
FROM eclipse-temurin:8-jre-jammy
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
