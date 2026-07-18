FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
ARG OTEL_JAVAAGENT_VERSION=2.29.0
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
RUN mvn -B -q dependency:copy \
    -Dartifact=io.opentelemetry.javaagent:opentelemetry-javaagent:${OTEL_JAVAAGENT_VERSION} \
    -DoutputDirectory=/otel \
    -Dmdep.stripVersion=true
COPY src src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/sgd-api-*.jar app.jar
COPY --from=build /otel/opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh
EXPOSE 8080
ENTRYPOINT ["/app/docker-entrypoint.sh"]
