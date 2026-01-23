Kanban with Realtime Collaboration — 10‑Week Teaching Project Plan

Goal
- Build a Spring Boot Kanban backend with realtime updates, security, and production-style tooling, entirely on local dependencies. Students finish with a demoable service and an interview-ready story.

Fixed Tech Stack
- Java 21, Spring Boot 3, Gradle or Maven.
- Postgres + Flyway; Redis (pub/sub, caching); H2 for fast tests.
- WebSocket or SSE for realtime; JWT for auth; Docker Compose for infra.
- Testing: JUnit 5, AssertJ, Testcontainers.
- Observability: Micrometer + Prometheus + Grafana (local).

Week-by-Week Milestones
- Week 1 — Skeleton & Domain: Boot app, Flyway baseline, entities (User, Board, Column, Card, Comment, Attachment, Membership). OpenAPI stub; global error/validation.
- Week 2 — Auth & RBAC: JWT login/refresh, password hashing, board membership roles; seed users; security filters and CORS.
- Week 3 — Core CRUD: Boards/columns/cards/comments CRUD with pagination and search; optimistic locking; soft deletes; indexes.
- Week 4 — Realtime Channel: WebSocket/SSE endpoints; Redis pub/sub broadcasting board events; client echo suppression; integration tests for two clients.
- Week 5 — Collaboration Rules: Ordering with position gaps, bulk move (drag/drop) with transactions; concurrency conflict responses and guidance.
- Week 6 — Attachments: Local storage under /data/uploads; size/MIME validation; short-lived download tokens; simple antivirus stub.
- Week 7 — Activity & Notifications: Append-only audit log; notifications stream; read/unread tracking.
- Week 8 — Observability & Perf: Metrics, structured logs with correlation IDs, basic rate limiting, board query caching; local load test and tuning notes.
- Week 9 — CI Simulation & Ops: Local CI script (lint, unit, integration, mutation optional); Docker Compose stack; Makefile/justfile shortcuts; onboarding doc.
- Week 10 — Hardening & Demo: Backup/restore drill, security review (CORS, tokens, path traversal), migration rollback test; prep final demo and interview narrative.

Assessment Artifacts
- README with architecture diagram and run instructions.
- OpenAPI docs, coverage report, load-test summary, security checklist, postmortem.
- Short demo script: two clients collaborating in realtime with conflict handling.

Job-Market Relevance
- Shows mastery of Spring Boot, JWT security, WebSocket/SSE, Redis pub/sub, Postgres schema design, migrations, automated testing with Testcontainers, observability, Dockerized local stacks, and ability to explain trade-offs in interviews.
