# Multi-stage Dockerfile for Mottu Smart Ops Core API (Java 21)

# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B || true

COPY src/ src/
RUN ./mvnw package -DskipTests -B

# Stage 2: Minimal Production JRE Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

COPY --from=builder /build/target/core-api-*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --retries=5 --start-period=30s \
  CMD curl -f http://localhost:8080/api/v1/auth/public-config || exit 1

ENTRYPOINT ["java", "-XX:+UseSerialGC", "-Xmx220m", "-Xms64m", "-XX:MaxMetaspaceSize=96m", "-jar", "app.jar"]

