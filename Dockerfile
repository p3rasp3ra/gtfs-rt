# =============================================================================
# Multi-stage build for GTFS-RT Spring Boot application
# Optimized for fast builds with layer caching
# =============================================================================

# Stage 1: Build
FROM eclipse-temurin:24-jdk-alpine AS builder

WORKDIR /app

# Install required tools for Gradle wrapper
RUN apk add --no-cache bash

# Copy Gradle wrapper first (rarely changes - cached layer)
COPY gradlew .
COPY gradle gradle
RUN chmod +x gradlew

# Copy build files (changes occasionally)
COPY build.gradle settings.gradle ./

# Download dependencies (cached unless build.gradle changes)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code (changes frequently - last layer)
COPY src src

# Build the application
RUN ./gradlew clean bootJar -x test --no-daemon

# Stage 2: Runtime (minimal image)
FROM eclipse-temurin:24-jre-alpine

WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the built jar from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Change ownership to spring user
RUN chown spring:spring app.jar

USER spring:spring

# Application port
EXPOSE 8088

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 --sun-misc-unsafe-memory-access=allow"

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD curl -f http://localhost:8088/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

