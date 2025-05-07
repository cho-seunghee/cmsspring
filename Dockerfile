FROM openjdk:21-jdk-bullseye AS builder

USER root

# 시스템 패키지 설치
RUN apt-get update && apt-get install -y findutils && rm -rf /var/lib/apt/lists/*

# 작업 디렉토리 설정
WORKDIR /app

# Gradle 파일 복사 및 실행 권한 부여
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY gradle.properties .
RUN chmod +x gradlew  # ← 여기!

# 소스 코드 복사
COPY src src

# Gradle 빌드
RUN ./gradlew clean bootJar --no-daemon

# 실행 스테이지
FROM openjdk:21-jdk-bullseye AS runner

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
