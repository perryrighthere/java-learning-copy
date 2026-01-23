---
name: kanban-backend-dev
description: Implement and iterate the Spring Boot Kanban backend with realtime collaboration, local-only dependencies, JWT security, Redis pub/sub, and Postgres; use when coding features, testing, or making design decisions.
---

# Quick Start
- Read `TECHNICAL_PLAN.md` for the architecture; `PROJECT_PLAN.md` for weekly scope.
- Stack: Java 21, Spring Boot 3, Gradle/Maven, Postgres, Redis, Flyway, WebSocket/SSE, JUnit 5, Testcontainers, Micrometer.
- Keep everything local (Docker Compose for Postgres/Redis/Prometheus/Grafana).

# Build Tasks (reference during development)
- **Auth/RBAC**: JWT access (15m) + refresh (7d); bcrypt; roles admin/member/guest enforced via Spring Security annotations. Cache membership lookups.
- **Domain**: Entities per technical plan; columns/cards use `position` gaps; cards use `@Version` for optimistic locking; soft delete with `deleted_at`.
- **API**: REST under `/api/v1`; implement move/reorder endpoints; consistent error model (Problem+JSON); OpenAPI generated.
- **Realtime**: Board channel `/topic/board.{id}` (WebSocket/STOMP or SSE). Publish domain events to Redis; relay to clients; include `eventId` + `resourceVersion`; drop/ask resync if buffer overflows.
- **Attachments**: Store under `./data/uploads/{uuid}`; validate MIME/size; short-lived download token (JWT or Redis TTL). Reject path traversal.
- **Audit/Notifications**: Append-only audit per board; notifications persisted + pushed; read/unread endpoints.
- **Observability**: Micrometer → Prometheus; structured logs with correlation IDs; rate limiting on auth/write; caching on board/membership reads.

# Testing & Quality
- Use Testcontainers for Postgres/Redis in integration tests; contract tests for WebSocket messages; RestAssured for REST.
- Run mutation testing (PIT) optionally in CI profile.
- Validate migrations with Flyway before merge; add indexes for position lookups.

# Local Tooling (suggested)
- Make/just targets: `dev` (run app with profiles), `test` (unit+integration), `ci` (lint+tests+mutation), `up`/`down` (docker-compose stack).
- Profiles: `dev`, `test`, `ci`; configure via `.env`.

# Decision Playbook
- WebSocket vs SSE: prefer WebSocket unless infra forbids; SSE acceptable if simple, document choice.
- Conflict handling: return 409 with latest entity; client retries with user prompt.
- Ordering: position gaps (e.g., 100, 200). Rebalance only when gaps exhausted.
- Security hardening: strict CORS, rate limit auth, limit attachment size, log audit trails.

# Demo Checklist
- Two clients connected to same board see live card move; conflict demo (409) shows guidance.
- Attachment upload + tokenized download; audit log entry created.
- Metrics visible in Grafana; health/readiness endpoints pass.***
