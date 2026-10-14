# Multi-stage Dockerfile for BankCore
# Stage 1: Build stage with Maven and Eclipse Temurin JDK 17
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy application source code and package
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal JRE Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root user for enterprise container security
RUN addgroup -S bankcore && adduser -S bankcore -G bankcore
USER bankcore

# Copy compiled jar from build stage
COPY --from=build /app/target/bankcore-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
