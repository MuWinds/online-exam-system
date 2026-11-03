FROM maven:3.9.11-eclipse-temurin-17 AS build

WORKDIR /workspace/backend
COPY backend/pom.xml .
COPY backend/src ./src
COPY sql /workspace/sql

RUN mvn -B resources:resources compiler:compile jar:jar spring-boot:repackage

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /workspace/backend/target/online-exam-backend-1.0.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
