# Multi-stage build for Spring Boot application
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copy source code
COPY src src

# Build the application
RUN ./gradlew clean build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built jar from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose application port
EXPOSE 8085

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8085/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

