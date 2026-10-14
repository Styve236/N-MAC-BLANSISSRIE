# --- Étape 1 : compilation ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
# Le wrapper Maven doit être exécutable sous Linux (perdu lors d'un checkout Windows)
RUN chmod +x mvnw && ./mvnw -B -DskipTests clean package

# --- Étape 2 : image d'exécution légère ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Render fournit le port via la variable PORT (l'app lit ${PORT})
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
