# Build Stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
ARG SKIP_TESTS=false
RUN if [ "$SKIP_TESTS" = "true" ]; then mvn clean package -Dmaven.test.skip=true -Djacoco.skip=true; else mvn clean package -DskipTests=false -Djacoco.skip=true; fi

# Run Stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY docker/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

EXPOSE 8080
ENTRYPOINT ["/app/entrypoint.sh"]
