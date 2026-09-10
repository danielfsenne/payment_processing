FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw -B dependency:go-offline
COPY src ./src
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN addgroup --system app && adduser --system --ingroup app --no-create-home app
COPY --from=build /app/target/*.jar app.jar
RUN chown app:app app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
