# Backstage Query Translator Proxy Service

Reactive Spring Boot (WebFlux) proxy that accepts Backstage-style structured
filter queries, translates them into conventional REST query parameters, fans
out to a downstream service to satisfy `OR` semantics, and returns merged,
deduplicated, paginated results.

Implements the PRD in
[`PRD-Backstage-Query-Translator-Proxy-Service.md`](PRD-Backstage-Query-Translator-Proxy-Service.md)
following a **CQRS + Hexagonal (Ports & Adapters)** architecture.

## Architecture

```
HTTP → RestControllerQueryTranslatorInboundAdapter (implements TranslateQueryPort)
     → DefaultTranslateQueryUseCase (parse → QueryPlan)
     → FilterGroupToQueryParamConverter
     → DownstreamQueryPort  ← RestApiDownstreamAdapter (WebClient, retry/timeout)
     → DefaultMergeResultsUseCase (dedupe by id, paginate, publish QueryTranslatedEvent)
     → HTTP response
```

Top-level packages: `command/` (bounded contexts), `core/` (config, credentials,
cron, domain, utils), `testing/` (test adapters mirroring command structure).

## Requirements

- Java 21
- Gradle (wrapper included — use `./gradlew`)

## Build & test

```bash
./gradlew check          # compile, all tests, ArchUnit, Karate, 90% coverage gate
./gradlew test           # unit + integration + Karate
./gradlew jacocoTestReport
# coverage report: build/reports/jacoco/test/html/index.html
```

Coverage gate: **≥ 90% line** (`jacocoTestCoverageVerification`).

## Run

```bash
DOWNSTREAM_BASE_URL=http://downstream:9090 ./gradlew bootRun
```

### Example request

```
GET /translator/entities
  ?filter=spec.type=image,spec.appId=CLAUT   # AND group
  &filter=spec.available=true                # OR group
  &limit=50&offset=0&sort=metadata.name&order=asc
Authorization: Bearer <token>
```

Returns `{ items, total, degraded, page }`. A failed downstream OR-group yields a
partial result with `degraded: true` rather than failing the whole request.

## Testing layers

| Layer | Location |
|-------|----------|
| Unit (parser, converter, merge) | `core/utils`, `command/.../domain/converter`, `usecases` |
| Reactive (StepVerifier) | `usecases` |
| Outbound adapter (MockWebServer) | `adapters/outbound` |
| Integration (`@SpringBootTest` + WebTestClient) | `adapters/inbound` |
| Architecture (ArchUnit) | `architecture` |
| API / e2e (Karate) | `src/test/resources/karate`, runner `karate/KarateApiTest` |

## Configuration

See `src/main/resources/application.yml` — downstream base URL, id field,
timeout, retry, cache, and Actuator endpoints (`/actuator/health`,
`/actuator/prometheus`).
