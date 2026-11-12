FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY riderService/pom.xml ./pom.xml
COPY riderService/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/target/*.jar /app/app.jar
USER 10001:10001
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
