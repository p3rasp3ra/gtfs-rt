# Spring Boot Guidelines

## 1. Prefer Constructor Injection over Field/Setter Injection
* Declare all the mandatory dependencies as `final` fields and inject them through the constructor.
* Spring will auto-detect if there is only one constructor, no need to add `@Autowired` on the constructor.
* Avoid field/setter injection in production code.

## 2. Prefer package-private over public for Spring components
* Declare Controllers, their request-handling methods, `@Configuration` classes and `@Bean` methods with default (package-private) visibility whenever possible. There's no obligation to make everything `public`.

## 3. Organize Configuration with Typed Properties
* Group application-specific configuration properties with a common prefix in `application.properties` or `.yml`.
* Bind them to `@ConfigurationProperties` classes with validation annotations so that the application will fail fast if the configuration is invalid.
* Prefer environment variables instead of profiles for passing different configuration properties for different environments.

## 4. Define Clear Transaction Boundaries
* Define each Service-layer method as a transactional unit.
* Annotate query-only methods with `@Transactional(readOnly = true)`.
* Annotate data-modifying methods with `@Transactional`.
* Limit the code inside each transaction to the smallest necessary scope.


## 5. Disable Open Session in View Pattern
* While using Spring Data JPA, disable the Open Session in View filter by setting ` spring.jpa.open-in-view=false` in `application.properties/yml.`

## 6. Separate Web Layer from Persistence Layer
* Don't expose entities directly as responses in controllers.
* Define explicit request and response record (DTO) classes instead.
* Apply Jakarta Validation annotations on your request records to enforce input rules.

## 7. Follow REST API Design Principles
* **Versioned, resource-oriented URLs:** Structure your endpoints as `/api/v{version}/resources` (e.g. `/api/v1/orders`).
* **Consistent patterns for collections and sub-resources:** Keep URL conventions uniform (for example, `/posts` for posts collection and `/posts/{slug}/comments` for comments of a specific post).
* **Explicit HTTP status codes via ResponseEntity:** Use `ResponseEntity<T>` to return the correct status (e.g. 200 OK, 201 Created, 404 Not Found) along with the response body.
* Use pagination for collection resources that may contain an unbounded number of items.
* The JSON payload must use a JSON object as a top-level data structure to allow for future extension.
* Use snake_case or camelCase for JSON property names consistently.

## 8. Use Command Objects for Business Operations
* Create purpose-built command records (e.g., `CreateOrderCommand`) to wrap input data.
* Accept these commands in your service methods to drive creation or update workflows.

## 9. Centralize Exception Handling
* Define a global handler class annotated with `@ControllerAdvice` (or `@RestControllerAdvice` for REST APIs) using `@ExceptionHandler` methods to handle specific exceptions.
* Return consistent error responses. Consider using the ProblemDetails response format ([RFC 9457](https://www.rfc-editor.org/rfc/rfc9457)).

## 10. Actuator
* Expose only essential actuator endpoints (such as `/health`, `/info`, `/metrics`) without requiring authentication. All the other actuator endpoints must be secured.

## 11. Internationalization with ResourceBundles
* Externalize all user-facing text such as labels, prompts, and messages into ResourceBundles rather than embedding them in code.

## 12. Use Testcontainers for integration tests
* Spin up real services (databases, message brokers, etc.) in your integration tests to mirror production environments.

## 13. Use random port for integration tests
* When writing integration tests, start the application on a random available port to avoid port conflicts by annotating the test class with:

    ```java
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    ```

## 14. Logging
* **Use a proper logging framework.**  
  Never use `System.out.println()` for application logging. Rely on SLF4J (or a compatible abstraction) and your chosen backend (Logback, Log4j2, etc.).

* **Protect sensitive data.**  
  Ensure that no credentials, personal information, or other confidential details ever appear in log output.

* **Guard expensive log calls.**  
  When building verbose messages at `DEBUG` or `TRACE` level, especially those involving method calls or complex string concatenations, wrap them in a level check or use suppliers:

```java
if (logger.isDebugEnabled()) {
    logger.debug("Detailed state: {}", computeExpensiveDetails());
}

// using Supplier/Lambda expression
logger.atDebug()
	.setMessage("Detailed state: {}")
	.addArgument(() -> computeExpensiveDetails())
    .log();
```

## 15. Virtual Threads Configuration and Best Practices
* Enable virtual threads in `application.yml`:
  ```yaml
  spring:
    threads:
      virtual:
        enabled: true
  ```
* Use `@Async` with virtual thread executor for I/O-bound operations outside controllers:
  ```java
  @Async("virtualThreadExecutor")
  public CompletableFuture<Void> processAsync(Data data) { ... }
  ```
* Avoid blocking operations in virtual threads that hold platform thread resources (synchronized blocks, Thread.sleep).
* Prefer reactive Redis operations over blocking calls when using virtual threads.

## 16. Implement Resiliency Patterns
* Use **Resilience4j** for retry mechanisms and circuit breakers:
  ```java
  @Retryable(value = {RedisConnectionFailureException.class}, maxAttempts = 3)
  @CircuitBreaker(name = "gtfs-data-service", fallbackMethod = "fallbackMethod")
  ```
* Implement fallback methods for external service dependencies.
* Configure appropriate timeouts for all external calls (Redis, PostgreSQL, GTFS Data Service).
* Use bulkhead pattern to isolate critical resources.

## 17. Observability and Monitoring
* Configure **Micrometer** with distributed tracing for message flow tracking:
  ```yaml
  management:
    tracing:
      sampling:
        probability: 1.0
  ```
* Create custom metrics for business operations:
  ```java
  @Timed(name = "gtfs.processing.time", description = "Time taken to process GTFS-RT message")
  ```
* Add structured logging with correlation IDs for request tracing.
* Monitor Redis cache hit ratios, Kafka consumer lag, and MQTT publish rates.

## 18. Cache Management Strategy
* Define explicit TTL policies for Redis entries:
  ```java
  @CacheEvict(value = "vehiclePositions", key = "#vehicleId", beforeInvocation = true)
  ```
* Implement cache warming for frequently accessed static GTFS data.
* Use Redis keyspace notifications for real-time cache invalidation.
* Configure appropriate Redis memory policies (allkeys-lru for GTFS-RT data).

## 19. MQTT and Kafka Integration Best Practices
* Use **Kafka Connect** with MQTT Source Connector for bridging MQTT to Kafka topics.
* Implement proper error handling and dead letter queues for message processing failures.
* Configure appropriate consumer group settings for parallel processing:
  ```yaml
  spring:
    kafka:
      consumer:
        max-poll-records: 100
        fetch-min-size: 1024
  ```
* Use schema registry for Protobuf message evolution and compatibility.

## 20. Security Hardening
* Implement JWT token validation with proper expiration and refresh logic.
* Use RBAC (Role-Based Access Control) for MQTT topic authorization.
* Enable TLS 1.3 for all external communications.
* Implement rate limiting per client/driver to prevent abuse:
  ```java
  @RateLimiter(name = "api", fallbackMethod = "rateLimitFallback")
  ```

## 21. Performance Optimization
* Configure connection pooling for all database connections:
  ```yaml
  spring:
    datasource:
      hikari:
        maximum-pool-size: 20
        minimum-idle: 5
  ```
* Use batch operations for bulk database writes in HistoryWriter.
* Implement message batching for MQTT publishing to reduce network overhead.
* Consider using Redis pipelining for multiple operations.

## 22. Graceful Shutdown and Health Checks
* Implement proper shutdown hooks for MQTT connections and Kafka consumers:
  ```java
  @PreDestroy
  public void shutdown() {
      mqttClient.disconnect();
      kafkaConsumer.close();
  }
  ```
* Configure readiness and liveness probes for Kubernetes:
  ```yaml
  management:
    endpoint:
      health:
        probes:
          enabled: true
  ```
* Add custom health indicators for external dependencies (Redis, PostgreSQL, MQTT broker).

## 23. Error Handling and Recovery
* Implement proper exception hierarchy for domain-specific errors.
* Use dead letter topics for unprocessable GTFS-RT messages.
* Add message replay capability for failed processing scenarios.
* Log structured error information for debugging and monitoring.

# Public API Service Implementation Specification

## 1. Project Goal

Implement the `Public API Service` for serving aggregated GTFS-RT data across two feeds (HTTP/Pull and MQTT/Push) from Redis, utilizing Java Virtual Threads (Project Loom) for high-performance I/O concurrency, while adhering to strong security and best practices.

## 2. Technology Stack & Constraints

| Component | Tool / Constraint | Rationale |
| :--- | :--- | :--- |
| **Base Framework** | Spring Boot 3+ (Spring MVC) | Simplicity and Virtual Threads compatibility. |
| **Concurrency** | **Java Virtual Threads (Project Loom)** | High concurrency for I/O-bound Redis and network operations. |
| **API Format** | **Protocol Buffers (GTFS-RT)** | Required binary serialization for efficiency. |
| **Data Source** | Spring Data Reactive Redis | Fast, non-blocking access to live state from the cache. |
| **Dependencies** | **GTFS Data Service** (Internal REST/gRPC) | Required by processing services for enriching RT messages. |
| **HTTP Headers** | **Must implement HTTP 304 Caching** | GTFS-RT Best Practice for bandwidth and server load reduction. |

## 3. Core Service Components & Responsibilities

### A. GTFS-RT Aggregator Component (API Internal)
* **Purpose:** Reads the latest live state from the cache layer.
* **Logic:** Executes **concurrent Redis lookups** across all three keyspaces (`vp:*`, `tu:*`, `sa:*`). Aggregates all entities into a single Protobuf `FeedMessage`.
* **Timestamp:** `FeedHeader.timestamp` must be set to the most recent update time across all fetched entities.

### B. HTTP Endpoint (`/gtfs-rt/feed.pb`) - The Pull Model
* **Endpoint:** `GET /gtfs-rt/feed.pb`
* **Protocol:** Must support TLS (HTTPS).
* **304 Caching Logic (Critical):**
    1.  Check the incoming `If-Modified-Since` header.
    2.  If the client's timestamp is current, return **HTTP 304 Not Modified**.
    3.  Otherwise, return the PBF payload with the `Last-Modified` header set to the `FeedMessage`'s timestamp.

### C. MQTT Publisher Component - The Push Model
* **Mechanism:** A background **reactive task** monitors Redis keyspace notifications for changes in VP, TU, or SA.
* **Action:** On detection of change, calls the Aggregator, serializes the PBF payload, and **Publishes** it to the public MQTT topic.
* **Rate Limiting:** Implement internal rate limiting (e.g., max 1 update per 5 seconds) to stabilize the public MQTT stream.

## 4. Security and Deployment

### A. Ingestion Security (MQTT Broker - MQS)
* **Transport Security:** **TLS 1.2+** must be enforced for all connections to the MQS (port 8883).
* **Authentication:** The MQS must use a **JWT Authentication Plugin**.
    * **Client Credentials:** The Ionic client connects using the **Driver ID** in the MQTT `USERNAME` field and a **short-lived JWT** (issued by the Auth Service) in the MQTT **`PASSWORD`** field.
* **Authorization (ACLs):** **Access Control Lists** are enforced based on the Driver ID extracted from the JWT payload, restricting clients to **publish only** to their assigned topic prefix (e.g., `ingest/vp/{DriverID}/#`).
* **New Service:** A dedicated **Authentication Service (AuthN)** must be developed to handle driver login and issue signed JWTs.

### B. GTFS Data Enrichment (GTFS Data Service)
* The `VP`, `TU`, and `SA` Processing Services must **query the GTFS Data Service** to correlate incoming GTFS-RT data (e.g., vehicle label, trip ID) with static GTFS data (e.g., route ID, trip start time) before persisting.

### C. Dockerization (All Services)
* **Dockerfile:** All microservices must use a multi-stage `Dockerfile` with a minimal Java runtime.
* **Base Image:** Must use a Java distribution supporting **Java 21+** to enable production use of Virtual Threads.
* **Configuration:** All service configuration must be managed via standard environment variables.

### D. Project layout

/gtfs-realtime-monolith
|
|-- /src/main/java/com/transit/gtfs
|   |-- Application.java             <-- The single main application entry point
|   |
|   |-- /auth                      <-- 🔑 Authentication (Login/JWT Issuance Logic)
|   |
|   |-- /data                      <-- 💾 Data Access & Repository Layer
|   |   |-- /gtfsstatic             <-- GTFS Data Service logic/clients
|   |   |-- /repository             <-- JPA/R2DBC repositories (Postgres)
|   |
|   |-- /ingestion                 <-- 📥 Kafka Consumers (Entry Point for RT data)
|   |   |-- /vpconsumer            <-- Vehicle Position consumer logic
|   |   |-- /tuconsumer            <-- Trip Update consumer logic
|   |   |-- /saconsumer            <-- Service Alert consumer logic
|   |
|   |-- /processing                <-- ⚙️ Core Business Logic & Persistence
|   |   |-- EnrichmentService.java   <-- Raw Protobuf -> Domain Model + Static Data Lookup
|   |   |-- StateUpdateService.java  <-- Writes current state to Redis cache
|   |   |-- HistoryWriter.java       <-- Writes enriched event to Postgres/TimescaleDB
|   |
|   |-- /distribution              <-- 📢 Public API & MQTT Output Flows
|   |   |-- ApiController.java       <-- HTTP GET /feed.pb endpoint (Virtual Threads)
|   |   |-- AggregatorService.java   <-- Assembles final PBF bundle from Redis
|   |   |-- MqttPublisher.java       <-- Manages publishing to public MQTT broker
|   |   |-- ChangeMonitorScheduler.java <-- Background task for the MQTT push feed
|   |
|   |-- /config                    <-- ⚙️ Global Configuration Classes (e.g., Kafka, Redis, Protobuf)
|   |
|   |-- /domain                    <-- 🧩 Shared Models and Entities (Domain Objects, JPA Entities)
|
|-- /src/main/resources/
|   |-- application.yml            <-- All system config (DB, Kafka, Redis, App)
|   |-- /proto/gtfs-rt.proto       <-- GTFS-RT schema file (used by the build tool)
|   |-- /db/migration/             <-- Liquibase/Flyway SQL scripts
|
|-- Dockerfile                       <-- Single multi-stage build file
|-- build.gradle/pom.xml             <-- All dependencies

### 5. Vehicle Position (VP) Processing Pipeline
The goal of this pipeline is to transform a raw, untrusted Protobuf message into a validated, enriched domain object and store it for both real-time access (Redis) and history (Postgres).

1. Ingestion Entry Point
   Package: /ingestion/vpconsumer
   Class: VpKafkaListener.java
   Action: This class is annotated with @KafkaListener and waits for a new message. It immediately passes the raw bytes to the business logic layer.
   Contract: handleVpMessage(byte[] rawProtobuf)

2. Core Business Logic and Enrichment
   Package: /processing

This step is the heart of the application, where validation and cross-referencing with static data occur.

Component	Responsibility	Action
EnrichmentService	Data Transformation & Validation	Receives the raw byte[]. Deserializes it into the Protobuf object. Extracts the vehicle ID and calls the GtfsStaticService to perform the static lookup (e.g., matching the vehicle's current location to a specific trip_id and route_id).
StateUpdateService	Real-time Caching	Receives the fully enriched domain object. Connects to Redis and performs a fast, non-blocking overwrite using the vehicle's key (vp:{agencyId}:{vehicleId}). This ensures the cache always holds the latest position.
HistoryWriter	Durable Persistence	Receives the enriched object. Writes the complete record (including its unique timestamp) to the Postgres/TimescaleDB tables. This is the slower, historical record path.

3. Data Infrastructure Layer
   Package: /data

This layer supports the processing stage by providing necessary lookups and handles the actual database commits.

Component	Responsibility	Action
/gtfsstatic	Static Data Lookup	Contains logic (e.g., GtfsDataService.java) that queries the /repository layer to perform geospatial or trip-matching queries against the static GTFS tables.
/repository	Persistence Abstraction	Contains the actual data access objects (VpTimescaleRepository.java). This layer handles the connection details and transactional commits to the PostgreSQL/TimescaleDB.

Data Flow Summary
The listener acts as the entry point, quickly pushing the data through dedicated services:
- Ingestion: rawProtobuf → VpKafkaListener
- Processing: EnrichmentService (Deserializes & Enriches via GtfsDataService lookup)
- Fork:
    - Fast Path: → StateUpdateService → Redis (Current State)
    - Slow Path: → HistoryWriter → Postgres/TimescaleDB (Historical Record)

### 6. Service Contracts & Key Methods

#### 1. Ingestion Contracts

| Package / Class | Purpose | Key Methods | Key Method Signatures (Contracts) |
| :--- | :--- | :--- | :--- |
| `ingestion/VpKafkaListener` | **VP Kafka Consumer** (High volume stream entry). | `handleVpMessage` | `@KafkaListener(topics = "gtfsrt.vp.raw")`<br> `public void handleVpMessage(@Payload byte[] rawProtobuf)` |
| `ingestion/TuKafkaListener` | **TU Kafka Consumer**. | `handleTuMessage` | `@KafkaListener(topics = "gtfsrt.tu.raw")`<br> `public void handleTuMessage(@Payload byte[] rawProtobuf)` |
| `ingestion/SaKafkaListener` | **SA Kafka Consumer**. | `handleSaMessage` | `@KafkaListener(topics = "gtfsrt.sa.raw")`<br> `public void handleSaMessage(@Payload byte[] rawProtobuf)` |

#### 2. Processing & Persistence

| Package / Class | Purpose | Key Methods | Key Method Signatures (Contracts) |
| :--- | :--- | :--- | :--- |
| `processing/EnrichmentService` | Converts raw Protobuf, validates, and queries static data. | `enrichAndValidate`<br> `getTripDescriptor` | `public VPDomain enrichAndValidate(byte[] rawProtobuf)`<br>`public TripDescriptor getTripDescriptor(String vehicleId)` |
| `processing/StateUpdateService` | Updates the current live status in Redis. | `updateCurrentState` | `public void updateCurrentState(VPDomain vp)` |
| `processing/HistoryWriter` | Writes the event to Postgres/TimescaleDB. | `persistHistory` | `public void persistHistory(VPDomain vp)` |
| `data/GtfsDataService` | **GTFS Static Data Source** (Query layer). | `getAgencyConfig`<br>`getTripInfo` | `public AgencyConfig getAgencyConfig(String agencyId)`<br>`public TripInfo getTripInfo(String tripId)` |
| `data/RedisReadRepository` | Queries Redis state for the Aggregator. | `findAllVpByAgency`<br>`getLastUpdateTime` | `public List<VPDomain> findAllVpByAgency(String agencyId)`<br>`public Long getLastUpdateTime(String agencyId)` |

#### 3. Distribution & Output

| Package / Class | Purpose | Key Methods | Key Method Signatures (Contracts) |
| :--- | :--- | :--- | :--- |
| `distribution/ApiController` | **HTTP Endpoint** for Google Transit (Pull). | `getFeed` | `@GetMapping("/gtfs-rt/feed.pb")`<br> `public ResponseEntity<byte[]> getFeed(@PathVariable String agencyId, @RequestHeader(IF_MODIFIED_SINCE) String since)` |
| `distribution/AggregatorService` | **Assembles the PBF payload** from all Redis states. | `buildFullFeed` | `public FeedMessage buildFullFeed(String agencyId)` |
| `distribution/MqttPublisher` | Manages the raw MQTT client connection and publishing. | `connect`<br>`publishBundle` | `public void connect(String username, String password)`<br>`public void publishBundle(String topic, byte[] payload)` |
| `distribution/ChangeMonitorScheduler` | Background task for the MQTT Push feed. | `publishAgencyBundles` | `@Scheduled(fixedRateString = "${app.mqtt.publish-rate}")`<br> `public void publishAgencyBundles()` |

