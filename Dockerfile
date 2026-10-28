# syntax=docker/dockerfile:1

# Build the executable Spring Boot JAR in an isolated, reproducible stage.
FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -DskipTests package

# Keep the runtime image small and avoid running the application as root.
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=build /workspace/target/payment-wallet-service-0.0.1-SNAPSHOT.jar app.jar
RUN chown spring:spring app.jar

USER spring
EXPOSE 8086

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
