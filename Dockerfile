FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

# Non-Alpine image: avoids musl/DNS quirks with Docker service hostnames (e.g. "postgres") on some hosts.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/devices-api-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
