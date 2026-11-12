# --- Etapa 1: compilar el jar con el Maven wrapper ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Primero solo lo necesario para bajar dependencias (se cachea entre builds)
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -q -B dependency:go-offline

COPY src src
RUN ./mvnw -q -B package -DskipTests

# --- Etapa 2: imagen liviana solo con el JRE ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/chatws-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
