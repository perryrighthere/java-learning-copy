---
name: kanban-course-guide
description: Run the 10-week Kanban with Realtime Collaboration teaching project; use when planning weekly milestones, grading artifacts, or mentoring students on scope, pacing, and deliverables.
---

# Usage
- Open this skill whenever scheduling/mentoring the 10-week project. It keeps scope aligned with the classroom timeline.
- For technical build details use `kanban-backend-dev`.

# Core References
- Consult `PROJECT_PLAN.md` (root) for the full milestone narrative and assessment artifacts.
- Use `TECHNICAL_PLAN.md` (root) if technical depth is needed while planning weeks.

# Weekly Milestone Cheatsheet
1. Skeleton & domain: app bootstrapped, Flyway baseline, entities outlined, OpenAPI stub, error handling.
2. Auth/RBAC: JWT login+refresh, hashed passwords, roles per board, demo seed users.
3. Core CRUD: boards/columns/cards/comments CRUD, pagination/search, optimistic locking, indexes.
4. Realtime: WebSocket/SSE channel per board, Redis pub/sub, echo suppression, integration test for two clients.
5. Collaboration rules: position-gap ordering, bulk move transactional, conflict handling (409 with latest version).
6. Attachments: local storage under `./data/uploads`, MIME/size checks, short-lived download tokens, AV stub.
7. Activity/notifications: append-only audit log, notification stream, read/unread tracking.
8. Observability/perf: metrics, structured logs, rate limiting, caching, load-test notes.
9. CI & ops: local CI script (lint/tests/mutation optional), Docker Compose stack, Make/just commands, onboarding doc.
10. Hardening/demo: backup/restore, migration rollback drill, security review, demo script and interview story.

# Assessment Artifacts to Require
- README with architecture diagram and run steps.
- OpenAPI docs; coverage report; load/perf summary; security checklist; postmortem; demo script showing realtime scenario.

# Classroom Tips
- Keep teams small (2–3) to surface coordination pain points.
- Require a short written “trade-off note” each time a major decision is made (e.g., WebSocket vs SSE).
- Schedule mid-project design review in Week 5 and perf/security review in Week 9.
- Encourage students to demo conflict resolution (409) and attachment download token flows.***
