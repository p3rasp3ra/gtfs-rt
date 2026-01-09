# Configuration Strategy for Host vs Docker Deployment

## Problem
The `spring-boot-docker-compose` dependency was automatically detecting the `compose.yaml` file and overriding connection settings to use Docker internal hostnames (`kafka:9093`, `timescaledb:5432`, etc.) instead of `localhost`, causing the app running on the host to fail.

## Proper Solution (Applied)

### 1. Removed Problematic Dependency
Removed `spring-boot-docker-compose` from `build.gradle`:
```groovy
// REMOVED: developmentOnly 'org.springframework.boot:spring-boot-docker-compose'
```

This dependency is useful for containerized apps but interferes when running on the host.

### 2. Fixed KafkaConfig to Read from Properties
Changed `KafkaConfig.java` from hardcoded values to properly inject configuration:
```java
@Value("${spring.kafka.bootstrap-servers}")
private String bootstrapServers;
```

Now it properly respects the configuration in `application.properties`.

### 3. Created Profile-Based Configuration

**Default Profile** (`application.properties`) - For running on HOST:
```properties
spring.kafka.bootstrap-servers=localhost:9093
spring.datasource.url=jdbc:postgresql://localhost:5432/gtfs_realtime_db
spring.data.redis.host=localhost
mqtt.url=tcp://localhost:1883
```

**Docker Profile** (`application-docker.properties`) - For running in CONTAINER:
```properties
spring.kafka.bootstrap-servers=kafka:9093
spring.datasource.url=jdbc:postgresql://timescaledb:5432/gtfs_realtime_db
spring.data.redis.host=redis
mqtt.url=tcp://mqtt-broker:1883
```

## How to Use

### Running on Host (Development)
```bash
# Default - uses localhost connections
./gradlew bootRun
```

### Running in Docker (Production)
```bash
# Activate docker profile - uses Docker internal hostnames
docker run -e SPRING_PROFILES_ACTIVE=docker your-app:latest
```

Or in `compose.yaml`:
```yaml
gtfs-rt-app:
  image: your-app:latest
  environment:
    - SPRING_PROFILES_ACTIVE=docker
```

## Benefits
✅ **No workarounds** - Clean, standard Spring Boot approach
✅ **Works in both environments** - Host and Docker
✅ **No manual changes needed** - Profile automatically applies correct config
✅ **Production-ready** - Follows Spring Boot best practices
✅ **Easy to maintain** - Clear separation of concerns

## Port Configuration Summary
```
Service                   Port    Host Access       Docker Access
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Spring Boot App          8085    localhost:8085    N/A (not in Docker)
Kafka UI                 8080    localhost:8080    kafka-ui:8080
Kafka Connect            8082    localhost:8082    kafka-connect:8082
Kafka                    9093    localhost:9093    kafka:9093
MQTT Broker              1883    localhost:1883    mqtt-broker:1883
Redis                    6379    localhost:6379    redis:6379
TimescaleDB              5432    localhost:5432    timescaledb:5432
```

