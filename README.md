## Kanban Realtime Backend (Week 1 + Week 2)

Spring Boot 3 / Java 21 teaching backend for a realtime Kanban project.
- Week 1 delivered skeleton/domain + Flyway + validation + OpenAPI stub.
- Week 2 delivered JWT auth/refresh, bcrypt password hashing, and board RBAC.

### Prerequisites
- JDK 21+
- Maven 3.9+

### Run locally (H2 file database)
```bash
mvn clean spring-boot:run
```
The app uses an H2 file database at `./data/kanban-db` with Flyway migrations applied automatically.

### Teaching frontend demo
- Open `http://localhost:8080/teaching-demo.html`
- Default root `http://localhost:8080/` redirects to the teaching demo page.
- The page includes:
  - Week 1 panel: health check + user creation
  - Week 2 panel: login/refresh + board read/member RBAC actions

### Tests
```bash
mvn test
```

### API/docs entry points
- Health: `GET http://localhost:8080/api/health`
- OpenAPI UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### Week 1 quick check (preserved)
This section is kept to preserve the Week 1 teaching progression.
- Create user (Week 1 payload): `POST http://localhost:8080/api/v1/users` with JSON `{"email":"alice@example.com","displayName":"Alice"}`
- Create board (Week 1 payload): `POST http://localhost:8080/api/v1/boards` with JSON `{"name":"Team Board","ownerId":1}`

Note: Week 1 examples are preserved historical context. Current Week 2 backend enforces JWT for board creation and derives owner from the authenticated user.

### Week 2 quick check (current behavior)
Seed users are created on startup with password `Password123!`:
- `alice@kanban.local` (board admin)
- `bob@kanban.local` (board member)
- `guest@kanban.local` (board guest/read-only)

Login:
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@kanban.local","password":"Password123!"}'
```

Use returned access token:
```bash
curl http://localhost:8080/api/v1/boards/1 \
  -H 'Authorization: Bearer <ACCESS_TOKEN>'
```

Refresh token:
```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H 'Content-Type: application/json' \
  -d '{"refreshToken":"<REFRESH_TOKEN>"}'
```

### Configuration notes
- JWT settings: `app.jwt.*` in `src/main/resources/application.yml`
- Allowed CORS origins: `app.security.cors.allowed-origins`
- Security is stateless; protected endpoints require `Authorization: Bearer <token>`

### Troubleshooting
- `401 Unauthorized`: token missing/expired/invalid, or refresh token used for protected API calls
- `403 Forbidden`: authenticated user lacks required board role
- Flyway/schema errors: ensure migrations under `src/main/resources/db/migration` are valid and ordered
