# Devices API

Spring Boot REST API for managing devices (name, brand, state, creation time), backed by PostgreSQL.

## Stack

- Java 21+, Maven 3.9+
- Spring Boot 3.4, JPA, Flyway, PostgreSQL
- Docker (Compose + Testcontainers for integration tests)

## Run locally

1. Start PostgreSQL:

   ```bash
   docker run -d --name devices-pg -p 5432:5432 \
     -e POSTGRES_DB=devices -e POSTGRES_USER=devices -e POSTGRES_PASSWORD=devices \
     postgres:16-alpine
   ```

2. Start the app:

   ```bash
   mvn spring-boot:run
   ```

3. Open Swagger UI at http://localhost:8080/swagger-ui/index.html

## Docker Compose

```bash
docker compose up --build
```

The app shares Postgres's network namespace (`network_mode: service:postgres`), so the JDBC URL uses `127.0.0.1:5432` internally. Port 8080 is published on the `postgres` container for this reason. Postgres is exposed to the host on port 5433 (to avoid conflicts with a local Postgres on 5432).

## API

All endpoints are under `/api/devices`.

| Method   | Path                | Description                                                        |
|----------|---------------------|--------------------------------------------------------------------|
| `POST`   | `/api/devices`      | Create device (`name`, `brand` required; `state` defaults to `AVAILABLE`) |
| `GET`    | `/api/devices/{id}` | Get one device                                                     |
| `GET`    | `/api/devices`      | List all (optional filters: `?brand=...`, `?state=...`, or both)   |
| `PUT`    | `/api/devices/{id}` | Full replace (`name`, `brand`, `state` required)                   |
| `PATCH`  | `/api/devices/{id}` | Partial update (any subset of `name`, `brand`, `state`)            |
| `DELETE` | `/api/devices/{id}` | Delete device                                                      |

**Domain rules:**
- Creation time is set once on create and cannot be changed. Sending `createdAt` on PATCH returns 400.
- Name and brand cannot be changed if the resulting state is `IN_USE`.
- `IN_USE` devices cannot be deleted.

**Error responses** use `{"error": "..."}` with status 400 (bad input), 404 (not found), or 409 (conflict).

## Tests

```bash
mvn verify
```

Runs unit tests (`DeviceRulesTest`) and integration tests (`DeviceApiIT` via Testcontainers). Integration tests are skipped automatically when Docker is unavailable.

To run against the Compose Postgres instead (stop the app container first so tests get a clean DB):

```bash
docker compose up -d && docker compose stop app
mvn verify -Pcompose-postgres
```

## Design decisions

| Requirement                        | Implementation                                                  |
|------------------------------------|-----------------------------------------------------------------|
| CRUD + list + filter by brand/state | `DeviceController` / `DeviceService`                           |
| Full and partial update            | `PUT` (full replace) and `PATCH` (partial, via `JsonNode`)      |
| Domain validations                 | `DeviceRules` (pure logic) + `@PrePersist` for creation time    |
| Persistent DB (not in-memory)      | PostgreSQL + JPA + Flyway migrations                            |
| Tests                              | `DeviceApiIT` (Testcontainers), `DeviceRulesTest` (unit)        |
| API documentation                  | springdoc OpenAPI + Swagger UI                                  |
| Containerized                      | Multi-stage `Dockerfile` + `docker-compose.yml`                 |

## Future improvements

- Pagination on list endpoints
- RFC 7807 problem details for error responses
- Readiness/liveness probes for Compose and Kubernetes

## Assumptions

- Omitted `state` on create defaults to `AVAILABLE`
- Brand filter is case-insensitive
- State values are `AVAILABLE`, `IN_USE`, `INACTIVE`
