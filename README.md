## Kanban Realtime Backend (Week 5)

Spring Boot 3 / Java 21 teaching backend with JWT auth, RBAC, CRUD, SSE realtime, and week 5 collaboration rules for column reorder and drag/drop card moves.

### Prerequisites
- JDK 21+
- Maven 3.9+
- Docker only if you want Redis-backed realtime locally or the Week 4 realtime integration test

### Run the app
```bash
mvn clean spring-boot:run
```

Default runtime:
- Port: `8080`
- DB: H2 in-memory for tests, H2 file at `./data/kanban-db` for local app runs
- Flyway migrations: `V1` to `V3`
- Realtime: SSE endpoint with optional Redis relay

### Start Redis for realtime fan-out
```bash
docker run --rm -p 6379:6379 redis:7.2-alpine
```

### Run tests
```bash
mvn test
```

Notes:
- `Week4RealtimeIntegrationTest` uses Testcontainers Redis and is skipped automatically when Docker is unavailable.
- Test runtime uses `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` with `mock-maker-subclass` so Mockito does not require JVM self-attachment.

### Configuration notes
Main config: `src/main/resources/application.yml`

Important properties:
- `app.jwt.secret`
- `app.jwt.issuer`
- `app.jwt.access-ttl`
- `app.jwt.refresh-ttl`
- `app.security.cors.allowed-origins`
- `app.realtime.redis-enabled`
- `app.realtime.redis-channel-prefix`
- `app.realtime.sse-timeout`
- `spring.data.redis.host`
- `spring.data.redis.port`

`X-Client-Id` is used for realtime echo suppression on write requests.

### API and docs entry points
- Health: `GET http://localhost:8080/api/health`
- OpenAPI UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Realtime SSE: `GET http://localhost:8080/api/v1/boards/{boardId}/events?clientId={id}`

Week 5 collaboration endpoints:
- `PATCH /api/v1/boards/{boardId}/columns/reorder`
- `PATCH /api/v1/cards/{cardId}/move`

### Teaching demo frontend
- URL: `http://localhost:8080/teaching-demo.html`
- The page now includes a guided showcase workspace plus Week 1 to Week 5 tabs.
- The guided workspace can:
  - generate a fresh demo user
  - create the user and log in
  - create a board and seed demo columns/cards
  - add a comment
  - run a card move and stale conflict demo without hand-entering IDs
- Week 5 adds:
  - transactional column reorder with ordered ID lists
  - neighbor-aware card move requests
  - stale conflict demos showing `409` guidance payloads

Seeded demo data on a clean database:
- board: `Week 2 Demo Board`
- columns: `To Do`, `Doing`
- cards: `Week 3 Sample Card`, `Week 3 Review`, `Week 5 Ordering Target`

### Week 5 quick checks
Login first:
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@kanban.local","password":"Password123!"}'
```

Reorder board columns:
```bash
curl -X PATCH http://localhost:8080/api/v1/boards/1/columns/reorder \
  -H 'Authorization: Bearer <ACCESS_TOKEN>' \
  -H 'Content-Type: application/json' \
  -d '{"orderedColumnIds":[2,1]}'
```

Move card `1` between cards `2` and `3` on a clean seed:
```bash
curl -X PATCH http://localhost:8080/api/v1/cards/1/move \
  -H 'Authorization: Bearer <ACCESS_TOKEN>' \
  -H 'Content-Type: application/json' \
  -d '{"targetColumnId":2,"previousCardId":2,"nextCardId":3,"version":0}'
```

Expected move/conflict behavior:
- successful move returns the updated card with new `columnId`, `position`, and `version`
- stale move returns `409` with `errorCode`, `retryable`, `guidance`, and `latest`

### Troubleshooting
- `401 Unauthorized`: missing or expired access token.
- `403 Forbidden`: current user lacks membership rights on the board.
- `409 Conflict`: refresh the returned `latest` state and retry with the newest version and neighbor IDs.
- Realtime test skipped: Docker is unavailable, so the Redis-backed Week 4 test is not executed.
- No SSE events: verify board access, `clientId`, Redis availability, and `app.realtime.redis-enabled=true`.
