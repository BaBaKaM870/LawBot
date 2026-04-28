# ── Stage 1 : compilation ───────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copier le pom séparément pour profiter du cache des dépendances
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Compiler et packager
COPY src ./src
RUN mvn package -DskipTests -B

# ── Stage 2 : image de production (JRE seul, ~250 MB) ───────────
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /app/target/lawbot-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
