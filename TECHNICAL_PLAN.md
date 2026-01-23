Kanban with Realtime Collaboration — Technical Plan

Architecture Overview
- Single Spring Boot service exposing REST + WebSocket/SSE.
- Postgres for persistence (Flyway migrations); Redis for pub/sub + caching.
- Local-only dependencies orchestrated via Docker Compose: app, postgres, redis, grafana, prometheus.
- Optional horizontal scaling: multiple app instances share Redis pub/sub; Postgres as single writer.

Modules / Packages
- auth: JWT issuance/refresh, password hashing, filters, CORS config.
- user: user profile, credentials, membership queries.
- board: boards, columns, cards, comments; ordering and optimistic locking.
- attachment: upload/download handlers, storage service, virus-scan stub.
- realtime: WebSocket/SSE controllers, Redis pub/sub bridge, event DTOs.
- audit: activity log persistence and query.
- notification: read/unread tracking and push events.
- infra: error handling, validation, OpenAPI config, observability, rate limiting.

Data Model (key tables)
- users(id, email, password_hash, created_at, updated_at)
- boards(id, name, owner_id, created_at, archived_at)
- memberships(id, user_id, board_id, role)
- columns(id, board_id, name, position, created_at)
- cards(id, column_id, title, description, position, version, created_at, updated_at, deleted_at)
- comments(id, card_id, author_id, body, created_at)
- attachments(id, card_id, filename, mime_type, size_bytes, storage_path, created_at)
- audit_log(id, board_id, actor_id, action, payload_json, created_at)
- notifications(id, user_id, board_id, kind, payload_json, read_at, created_at)

API Surface (REST, versioned under /api/v1)
- Auth: POST /auth/login, POST /auth/refresh.
- Users: GET /users/me, PATCH /users/me.
- Boards: CRUD /boards; membership CRUD /boards/{id}/members.
- Columns: CRUD /boards/{id}/columns; reorder endpoint PATCH /columns/reorder.
- Cards: CRUD /columns/{id}/cards; move/reorder endpoint PATCH /cards/{id}/move.
- Comments: CRUD /cards/{id}/comments.
- Attachments: POST multipart /cards/{id}/attachments; GET /attachments/{id}/download?token=.
- Audit: GET /boards/{id}/audit (paged).
- Notifications: GET /notifications, POST /notifications/{id}/read.

Realtime Design
- Transport: WebSocket (STOMP) or SSE; channels are board-scoped (`/topic/board.{boardId}`).
- Event types: card_created/updated/moved/deleted, column_reordered, comment_added, member_joined/left, notification_created.
- Fan-out: app publishes domain events to Redis channel; subscribers relay to connected clients.
- Idempotency: events carry `eventId` and `resourceVersion`; clients ignore stale versions.
- Backpressure: server drops if client buffer exceeds N, instructs client to resync via REST.

Security
- JWT access (15m) + refresh (7d); password hashing with bcrypt.
- RBAC roles: admin (board owner), member, guest (read-only).
- Method-level checks via Spring Security annotations; per-board membership lookup cached.
- Input validation with Bean Validation; global exception mapper returning Problem+JSON.
- CORS restricted to configurable origins; rate limiting filter (bucket4j) on auth and write endpoints.

Storage & Files
- Attachments stored under configurable `storage.root` (e.g., `./data/uploads`); path is derived from UUID to avoid traversal.
- Short-lived download token (signed JWT or random token persisted with TTL in Redis).
- Antivirus stub: simple MIME/size checks and optional ClamAV mock; failures logged and rejected.

Ordering & Concurrency
- Card/column ordering uses `position` as decimal gaps (e.g., 100, 200) to minimize rewrites.
- Optimistic locking on cards (`@Version`); conflict returns 409 with latest version payload.
- Bulk move executes in a single transaction; publishes a single aggregated event.

Caching & Performance
- Cache board/member lookups in Redis with short TTL.
- HTTP response compression; DB indexes on (board_id, position), (column_id, position), and common filters.
- Performance budgets: P99 < 200ms for common reads on dev hardware; WebSocket reconnect < 3s.

Observability
- Micrometer to Prometheus; Grafana dashboards for request rate/latency, DB pool, WebSocket sessions.
- Structured JSON logging with correlation IDs (from `X-Request-Id` or generated).
- Health and readiness probes at /actuator/health and /actuator/ready.

Testing Strategy
- Unit tests for services; slice tests for controllers with MockMvc.
- Integration tests with Testcontainers (Postgres, Redis) covering auth, CRUD, realtime publish.
- Contract tests for WebSocket messages (embedded broker) and REST (RestAssured).
- Mutation testing (PIT) optional gate in CI script.
- Load test profile (Gatling/Locust) run locally against Docker stack.

Local Environment & Tooling
- Docker Compose services: app, postgres, redis, prometheus, grafana.
- Common commands (Makefile/justfile):
  - `make dev` → run app with local Postgres/Redis.
  - `make test` → unit + integration tests.
  - `make ci` → lint/format, tests, mutation tests (optional).
  - `make up` / `make down` → docker-compose stack.
- Config via `.env` and Spring profiles: `dev`, `test`, `ci`.

Migrations & Data
- Flyway for schema; seed script inserts demo users/boards/cards for demos.
- Backup/restore script using `pg_dump`/`pg_restore` against local Postgres.
- Migration rollback drill in Week 10.

Demo & Interview Story
- Two-browser demo: user A and B on same board; A moves card, B sees realtime update; conflict scenario shows 409 with guidance; file upload + audit log visible.
- Talking points: why WebSocket over SSE (or vice versa), Redis pub/sub trade-offs, optimistic locking vs. pessimistic, handling attachment security locally.
