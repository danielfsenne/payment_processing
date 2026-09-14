FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Build context is the repo root (see docker-compose.yml) so this stage can also see
# common-security, a sibling module this service depends on. Install it into the
# image's local Maven repo first so the service's own build below can resolve it.
COPY services/common-security /common-security
RUN cd /common-security && chmod +x mvnw && ./mvnw -B -q install -DskipTests

COPY services/account-service/.mvn/ .mvn/
COPY services/account-service/mvnw services/account-service/pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw -B dependency:go-offline
COPY services/account-service/src ./src
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN addgroup --system app && adduser --system --ingroup app --no-create-home app
COPY --from=build /app/target/*.jar app.jar
RUN chown app:app app.jar
USER app
EXPOSE 8082
HEALTHCHECK --interval=10s --timeout=5s --start-period=240s --retries=6 \
    CMD curl -f http://localhost:8082/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
