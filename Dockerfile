# Use Gradle 8.10 with JDK 21 for building
FROM gradle:8.10-jdk21 AS builder

# Set working directory
WORKDIR /app

# Copy Gradle configuration file (Groovy DSL)
COPY build.gradle /app/
COPY gradle/wrapper/gradle-wrapper.properties /app/gradle/wrapper/
COPY gradle/wrapper/gradle-wrapper.jar /app/gradle/wrapper/

# Copy source code
COPY src /app/src

# Build the application (skip tests for faster builds)
RUN gradle build --no-daemon -x test

# Use OpenJDK 21 slim for running
FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/build/libs/cms-0.0.1-SNAPSHOT.jar app.jar

# Expose port 8080 (default for Spring Boot)
EXPOSE 8080

# Run the JAR
ENTRYPOINT ["java", "-jar", "app.jar"]