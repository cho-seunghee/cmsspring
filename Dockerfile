# ---- Build Stage ----
FROM openjdk:21-jdk-bullseye AS builder

USER root

# 필수 시스템 패키지 설치
RUN apt-get update && apt-get install -y findutils && rm -rf /var/lib/apt/lists/*

# 작업 디렉토리 설정
WORKDIR /app

# Gradle 설정 파일과 스크립트 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY gradle.properties .
# Gradle Wrapper 실행 권한 부여
RUN chmod +x gradlew

# 소스 코드 복사
COPY src src

# Gradle 빌드: JAR 파일 생성
RUN ./gradlew clean bootJar --no-daemon

# ---- Runtime Stage ----
FROM openjdk:21-jdk-bullseye AS runner

WORKDIR /app

# 빌드 스테이지에서 생성된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 컨테이너 외부로 노출할 포트 설정
EXPOSE 8080

# 애플리케이션 실행 명령 설정
ENTRYPOINT ["java", "-jar", "app.jar"]