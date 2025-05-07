# 1단계: 빌드 스테이지
FROM openjdk:21-jdk AS builder

# 시스템 패키지 설치
USER root
RUN apt-get update && apt-get install -y findutils && rm -rf /var/lib/apt/lists/*

# 작업 디렉토리 설정
WORKDIR /app

# Gradle 관련 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY gradle.properties .

# 소스 복사
COPY src src

# Gradle 빌드
RUN ./gradlew clean bootJar --no-daemon

# 2단계: 실행 스테이지
FROM openjdk:21-jdk-slim AS runner

# 작업 디렉토리 설정
WORKDIR /app

# 빌드된 JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 포트 노출 (필요 시)
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
