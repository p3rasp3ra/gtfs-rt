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