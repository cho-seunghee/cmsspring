# Use OpenJDK 21 for building
FROM openjdk:21-jdk AS builder

# Set working directory
WORKDIR /app

# Copy Gradle Wrapper, configuration, and properties
COPY gradle/ /app/gradle/
COPY gradlew /app/
COPY build.gradle /app/
COPY gradle.properties /app/

# Copy source code
COPY src /app/src

# Make Gradle Wrapper executable
RUN chmod +x gradlew

# Build the application with verbose output and forced dependency refresh
RUN ./gradlew build --no-daemon -x test --stacktrace --info --refresh-dependencies -Dorg.gradle.jvmargs="-Xmx2g -Xms512m" || { echo "Gradle build failed"; cat build/reports/*; exit 1; }

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