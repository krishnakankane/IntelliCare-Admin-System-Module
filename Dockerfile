# =============================================
# IntelliCare Backend — Multi-stage Dockerfile
# =============================================

# ===== STAGE 1: Build =====
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ===== STAGE 2: Runtime =====
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: run as non-root
RUN addgroup -S intellicare && adduser -S intellicare -G intellicare

WORKDIR /app

# Create log directory
RUN mkdir -p /var/log/intellicare && chown intellicare:intellicare /var/log/intellicare

COPY --from=builder /build/target/*.jar app.jar
RUN chown intellicare:intellicare app.jar

USER intellicare

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=50.0 \
               -XX:+UseG1GC \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=prod"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
