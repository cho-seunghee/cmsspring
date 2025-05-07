# 빌드 스테이지: Debian 기반 openjdk 사용
FROM openjdk:21-jdk AS builder

# root 권한으로 필요한 도구 설치
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

# 소스 코드 복사
COPY src src

# Gradle 빌드
RUN ./gradlew clean bootJar --no-daemon

# 실행 스테이지
FROM openjdk:21-jdk AS runner

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
