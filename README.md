# Devices API

Spring Boot REST API for **devices** (name, brand, state, creation time) backed by **PostgreSQL**, with OpenAPI docs, Docker Compose, and integration tests (Testcontainers).

## Stack

- Java **21+**, **Maven 3.9+**
- Spring Boot **3.4**, JPA, **PostgreSQL** (not in-memory)
- **Docker** for Compose and for `mvn verify` integration tests

## Run locally

1. PostgreSQL (example):

   ```bash
   docker run -d --name devices-pg -p 5432:5432 \
     -e POSTGRES_DB=devices -e POSTGRES_USER=devices -e POSTGRES_PASSWORD=devices \
     postgres:16-alpine
   ```

2. App (`src/main/resources/application.yml` matches the credentials above):

   ```bash
   mvn spring-boot:run
   ```

3. Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) · OpenAPI JSON: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

## Docker Compose

```bash
docker compose up --build
```

The app runs in **Postgres’s network namespace** (`network_mode: service:postgres`) so the JDBC URL uses **`127.0.0.1:5432`** inside the stack (avoids Docker DNS for `postgres`). **HTTP:** [http://localhost:8080](http://localhost:8080). **Postgres from your Mac:** **`localhost:5433`** → container `5432` (see `docker-compose.yml`; `5433` avoids fighting another Postgres on `5432`). Port **8080** is declared on **`postgres`** because that container owns the shared network stack.

**`Bind for …:5432 failed: port is already allocated`** means something on the host is already using that port (common: Homebrew Postgres, another Compose project, or an old container). Run `docker compose down --remove-orphans`, then `lsof -nP -iTCP:5432 -sTCP:LISTEN` (and `docker ps`) to see what holds it. With the default **`5433:5432`** mapping, Compose should start even if local Postgres uses **5432**.

If **8080** refuses connections: Postgres can be “up” while the **Java app has exited**. Run `docker compose ps` (expect **`app`** running, not `Exited`) and `docker compose logs app --tail=80`. Rebuild after code changes: `docker compose up --build --force-recreate`. Try **`http://127.0.0.1:8080`** if `localhost` behaves oddly (IPv6).

## API (`/api/devices`)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/devices` | Create (`name`, `brand` required; `state` optional, default `AVAILABLE`) |
| `GET` | `/api/devices/{id}` | One device |
| `GET` | `/api/devices` | All devices |
| `GET` | `/api/devices?brand=...` | By brand (case-insensitive) |
| `GET` | `/api/devices?state=...` | By `AVAILABLE`, `IN_USE`, or `INACTIVE` |
| `GET` | `/api/devices?brand=...&state=...` | Both filters |
| `PUT` | `/api/devices/{id}` | Full replace `name`, `brand`, `state` |
| `PATCH` | `/api/devices/{id}` | Partial JSON: `name`, `brand`, `state` only; `createdAt` → **400**; other unknown keys ignored |
| `DELETE` | `/api/devices/{id}` | Delete (forbidden when `IN_USE`) |

**Rules:** creation time is set on create only; **in-use** devices cannot change **name**/**brand** or be **deleted**.

**Errors:** `404` not found; `409` / `400` return `{"error":"..."}` for conflicts and bad input (e.g. invalid `state` on PATCH).

## Tests

**Default (self-contained Postgres via Testcontainers):**

```bash
mvn verify
```

Runs `DeviceRulesTest` (unit) and `DeviceApiIT` (`@Tag("testcontainers")`). `DeviceApiIT` is skipped when Docker is unavailable (`@Testcontainers(disabledWithoutDocker = true)`). `DeviceApiComposeIT` is excluded by default.

**Against Postgres from `docker compose` (host port `5433` by default):**

1. Start the stack: `docker compose up -d` (or at least Postgres reachable on the mapped port).
2. Prefer stopping the app so tests do not share the DB with a running API: `docker compose stop app`.
3. Run:

```bash
mvn verify -Pcompose-postgres
```

That profile runs `DeviceRulesTest` and `DeviceApiComposeIT` (`@Tag("compose")`), pointing at `jdbc:postgresql://localhost:5433/devices`. Override host/port with `RUN_COMPOSE_HOST` / `RUN_COMPOSE_PORT` if needed.

Shared scenarios live in `DeviceApiTestSupport` so both integration setups stay in sync.

## Challenge checklist (PDF)

| Requirement | How it is met |
|---------------|----------------|
| CRUD + list + filter by brand/state | `DeviceController` / `DeviceService` |
| Full and partial update | `PUT` and `PATCH` |
| Domain validations | `DeviceService` (`createdAt` only in entity `@PrePersist` / not set on update) |
| Java 21+, Maven | `pom.xml` |
| Persistent DB (not in-memory) | PostgreSQL + JPA |
| Tests | `DeviceApiIT` (Testcontainers), `DeviceApiComposeIT` (Compose, profile `compose-postgres`), `DeviceRulesTest` |
| API documented | springdoc + `@Operation` / `@Tag` |
| Containerized | `Dockerfile`, `docker-compose.yml` |
| README | This file |

## Layout

- `src/main/java/com/devices` — application, entity, repository, service, web, errors
- `src/test/java/com/devices` — tests
- `Dockerfile`, `docker-compose.yml`

## Future improvements

- Pagination on list; RFC 7807 problem details; Flyway/Liquibase instead of `ddl-auto: update`; readiness/liveness probes for Compose/Kubernetes.

## Assumptions

- Omitted `state` on create → `AVAILABLE`. Brand filter is case-insensitive; state values match the enum names above.
