# syntax=docker/dockerfile:1
# No BuildKit cache mounts: Railway rejects cache mounts without a hard-coded service-id prefix,
# so dependency caching relies on the go-offline layer, reused while pom.xml does not change.
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:25-jre
RUN useradd --system --create-home kalibra
USER kalibra
WORKDIR /app
COPY --from=build /build/target/kalibra-api-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
