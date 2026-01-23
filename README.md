## Kanban Realtime Backend (Week 1 Skeleton)

Spring Boot 3 / Java 21 starter for a realtime Kanban backend. This slice includes domain entities, Flyway baseline schema, validation, global error handling, and an OpenAPI stub with minimal user/board endpoints.

### Prerequisites
- JDK 21
- Maven 3.9+

### Run locally (H2 file database)
```bash
mvn clean spring-boot:run
```
The app uses an H2 file database at `./data/kanban-db` with Flyway migrations applied automatically.

### API quick check
- Health: `GET http://localhost:8080/api/health`
- OpenAPI UI: `http://localhost:8080/swagger-ui.html`
- Create user: `POST http://localhost:8080/api/v1/users` with JSON `{"email":"alice@example.com","displayName":"Alice"}`
- Create board: `POST http://localhost:8080/api/v1/boards` with JSON `{"name":"Team Board","ownerId":1}`

### Notes
- Validation errors return RFC7807 Problem Details with a per-field `errors` map.
- Schema changes should be done via Flyway migrations under `src/main/resources/db/migration`.
