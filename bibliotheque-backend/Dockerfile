# Contexte de build : bibliotheque-backend/ (voir docker-compose.yml).
# Le frontend Angular n'est plus embarqué dans le jar : il est servi par le
# service nginx « frontend ». Construire l'API ne demande donc plus Node.

# --- Étape 1 : build ------------------------------------------------------
# Le pom.xml cible Java 1.8 et embarque une version de Lombok trop ancienne
# pour l'API interne du compilateur des JDK 17+ (voir README section 2 :
# NoSuchFieldError sur JCTree$JCImport) : on construit avec un JDK 8.
FROM maven:3.8.6-eclipse-temurin-8 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests \
    # Jar en couches : dépendances, loader et code applicatif séparés. Une
    # modification du code ne reconstruit plus que la dernière couche.
    && mkdir -p target/couches \
    && cd target/couches \
    && java -Djarmode=layertools -jar ../*.jar extract

# --- Étape 2 : exécution --------------------------------------------------
FROM eclipse-temurin:8-jre-alpine
WORKDIR /app

# Utilisateur sans privilèges : une faille dans l'application ne donne pas
# les droits root dans le conteneur.
RUN addgroup -S bibliotheque && adduser -S -G bibliotheque bibliotheque

COPY --from=build /app/target/couches/dependencies/ ./
COPY --from=build /app/target/couches/spring-boot-loader/ ./
COPY --from=build /app/target/couches/snapshot-dependencies/ ./
COPY --from=build /app/target/couches/application/ ./

USER bibliotheque

# La JVM se dimensionne sur la limite mémoire du conteneur (512M) au lieu de
# la RAM de la machine, et s'arrête franchement (redémarrage Docker) plutôt
# que de continuer dans un état dégradé après un OutOfMemoryError.
# MaxRAMPercentage attend un nombre décimal : « 75 » empêche la JVM 8 de démarrer.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
