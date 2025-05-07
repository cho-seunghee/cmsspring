# 빌드 스테이지: Debian 기반 명시
FROM openjdk:21-jdk-bullseye AS builder

USER root
RUN apt-get update && apt-get install -y findutils && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY gradle.properties .
COPY src src

RUN ./gradlew clean bootJar --no-daemon

# 실행 스테이지
FROM openjdk:21-jdk-bullseye AS runner

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
