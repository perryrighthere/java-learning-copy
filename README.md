## Kanban Realtime Backend (Week 4)

Spring Boot 3 / Java 21 backend with JWT auth, RBAC, core CRUD, and board-scoped realtime updates over SSE backed by Redis pub/sub.

### Prerequisites
- JDK 21+
- Maven 3.9+
- Docker (for local Redis in Week 4 realtime mode)

### Start Redis (local)
```bash
docker run --rm -p 6379:6379 redis:7.2-alpine
```

### Run the app
```bash
mvn clean spring-boot:run
```

Default runtime:
- Port: `8080`
- DB: H2 file database at `./data/kanban-db`
- Flyway migrations: `V1`..`V3`
- Realtime: SSE endpoint with Redis pub/sub relay

### Run tests
```bash
mvn test
```

### Configuration notes
Main config: `src/main/resources/application.yml`

Key properties:
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

`X-Client-Id` request header is used for realtime echo suppression.

### API/docs entry points
- Health: `GET http://localhost:8080/api/health`
- OpenAPI UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Realtime SSE: `GET http://localhost:8080/api/v1/boards/{boardId}/events?clientId={id}`

### Teaching demo frontend
- URL: `http://localhost:8080/teaching-demo.html`
- Week 4 tab now includes:
  - two SSE clients on the same board
  - connect/disconnect controls
  - trigger buttons that send `X-Client-Id`
  - side-by-side event logs to verify echo suppression

### Realtime quick check (two terminals)
Login first:
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@kanban.local","password":"Password123!"}'
```

Terminal A subscribe:
```bash
curl -N "http://localhost:8080/api/v1/boards/1/events?clientId=client-a" \
  -H 'Authorization: Bearer <ACCESS_TOKEN>'
```

Terminal B subscribe:
```bash
curl -N "http://localhost:8080/api/v1/boards/1/events?clientId=client-b" \
  -H 'Authorization: Bearer <ACCESS_TOKEN>'
```

Trigger board event from client A:
```bash
curl -X POST http://localhost:8080/api/v1/columns/1/cards \
  -H 'Authorization: Bearer <ACCESS_TOKEN>' \
  -H 'X-Client-Id: client-a' \
  -H 'Content-Type: application/json' \
  -d '{"title":"Realtime demo card","description":"Week 4"}'
```

Expected behavior:
- Terminal B receives `card.created` event.
- Terminal A does not receive its own echoed event.

### Troubleshooting
- `401 Unauthorized`: missing/expired access token.
- `403 Forbidden`: user lacks board membership permission.
- `500` with Redis connection hints: ensure Redis is running and `spring.data.redis.*` points to it.
- No SSE events: verify board access, `clientId`, and `app.realtime.redis-enabled=true`.
