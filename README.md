# spring-boot-task-api

> Production Spring Boot 4 REST API. **973 req/s at p99 159ms** on a JWT-authenticated endpoint (single instance, H2 demo profile, 50 concurrent clients, 5000/5000 OK). JWT auth, Postgres + Flyway, bcrypt, per-user authorization, 14 tests on H2 with zero Docker needed.

[![ci](https://github.com/Tajaddin/spring-boot-task-api/actions/workflows/ci.yml/badge.svg)](https://github.com/Tajaddin/spring-boot-task-api/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)
[![Java](https://img.shields.io/badge/java-21-orange)](pom.xml)
[![Spring Boot](https://img.shields.io/badge/spring--boot-4.0-brightgreen)](pom.xml)

## Hero metrics

Reproducible with zero setup (no Docker, no Postgres):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo   # boots on in-memory H2
python load/load_probe.py --requests 5000 --concurrency 50
```

Last measured 3-run baseline (full output + hardware in [`bench/results.txt`](bench/results.txt)):

| Metric | Value |
|---|---:|
| **Throughput** | **973 req/s** (cold-start baseline; warm runs reach ~1,500 rps after JIT) |
| **Latency p99** | **159 ms** cold, ~60 ms after JIT warmup |
| Success rate | 5000 / 5000 (100%) per run |

Measured on `GET /api/tasks?size=20` (JWT-authenticated, paginated, owner-scoped query) against a single instance on the H2 demo profile, driven by the standard-library Python probe at concurrency 50. JIT warmup is visible across runs: run 1 lands near the 973 baseline; runs 2–3 climb to ~1.5K rps with p99 below 70 ms. The 973 number stays the conservative headline because that is what a fresh single-run measurement reproduces.

## What it is

A task-management REST API that demonstrates the patterns enterprise Java backends are built on:

| Concern | Implementation |
|---|---|
| Auth | Stateless JWT (HS256, jjwt 0.12), bcrypt password hashing, `/api/auth/register` + `/api/auth/login` |
| Authorization | Every task query is scoped to the authenticated user id; cross-user access returns 404 (no existence leak) |
| Persistence | Spring Data JPA + PostgreSQL in production, Flyway-managed schema (`V1__init.sql`) |
| Validation | Jakarta Bean Validation on request DTOs, structured 400 responses with per-field errors |
| Pagination | Spring `Pageable`, capped page size, sorted by created-at |
| Errors | `@RestControllerAdvice` mapping domain + validation exceptions to a consistent `ErrorResponse` |
| Observability | Spring Boot Actuator health / info / metrics / prometheus endpoints |
| Tests | 14 tests: JWT unit, service unit (Mockito), full HTTP flow via MockMvc on H2 + real Flyway migrations |

## Why this matters for hiring

Role categories unlocked: **Backend-Java**, enterprise software, microservices, full-stack (Java side).

Spring Boot is the single most-requested enterprise backend stack in the job market. This repo backs the "Java / Spring Boot" line on the resume with a real, tested, load-measured service, not a tutorial.

## How to run

Prerequisites: JDK 21+ (the included Maven wrapper handles the rest); Docker optional for the Postgres compose stack.

```bash
./mvnw test                                                    # 14 tests on H2 demo profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo         # API on :8080 (H2)
python load/load_probe.py --requests 5000 --concurrency 50     # reproduces the rps hero
```

### Zero-setup demo (H2, in-memory)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
# API on http://localhost:8080
```

```bash
# register
curl -s -XPOST localhost:8080/api/auth/register \
  -H 'content-type: application/json' \
  -d '{"email":"me@example.com","password":"password123"}'
# -> {"token":"eyJ...","tokenType":"Bearer","expiresInSeconds":3600}

TOKEN=...   # paste the token

# create a task
curl -s -XPOST localhost:8080/api/tasks \
  -H "authorization: Bearer $TOKEN" -H 'content-type: application/json' \
  -d '{"title":"Ship the API","priority":"HIGH"}'

# list (paginated, owner-scoped)
curl -s localhost:8080/api/tasks -H "authorization: Bearer $TOKEN"
```

### Production stack (Postgres via Docker)

```bash
docker compose up --build
# app on :8080, Postgres on :5432, Flyway migrates on boot
```

## API

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | none | Create account, returns JWT |
| POST | `/api/auth/login` | none | Exchange credentials for JWT |
| GET | `/api/tasks` | Bearer | List own tasks (params: `status`, `page`, `size`) |
| POST | `/api/tasks` | Bearer | Create a task |
| GET | `/api/tasks/{id}` | Bearer | Get one own task |
| PUT | `/api/tasks/{id}` | Bearer | Partial update (null fields untouched) |
| DELETE | `/api/tasks/{id}` | Bearer | Delete own task |
| GET | `/actuator/health` | none | Liveness / readiness |

## Testing

```bash
./mvnw test          # 14 tests, runs on H2 in PostgreSQL mode, no Docker
```

- `JwtServiceTest` (4): issue/parse round trip, TTL, tamper rejection, foreign-key rejection.
- `TaskServiceTest` (4): create defaults, not-found on unowned, partial-update semantics, delete guard. Mockito, no Spring context.
- `ApiIntegrationTest` (5): full MVC + security filter chain on H2 — anonymous rejected, register/login/CRUD, cross-user isolation, duplicate-email rejection, password validation.
- `SpringBootTaskApiApplicationTests` (1): context loads with Flyway migrations applied.

JaCoCo coverage report at `target/site/jacoco/index.html` after `./mvnw test`.

## Load testing

```bash
# lightweight, no deps:
python load/load_probe.py --requests 5000 --concurrency 50

# heavier, scriptable (needs k6 + the compose stack):
docker compose up -d
k6 run load/k6-script.js
```

## Project layout

```
src/main/java/com/tajaddin/taskapi/
  config/SecurityConfig.java          # stateless JWT filter chain
  security/JwtService.java            # HS256 issue + verify (jjwt 0.12)
  security/JwtAuthenticationFilter.java
  security/CurrentUser.java           # reads user id from the security context
  user/  User, UserRepository, AuthService, AuthController, dto/
  task/  Task, TaskStatus, TaskPriority, TaskRepository, TaskService, TaskController, dto/
  common/GlobalExceptionHandler.java  # consistent ErrorResponse
src/main/resources/
  application.yml                     # default (Postgres) profile
  application-demo.yml                # H2 demo profile (zero setup)
  db/migration/V1__init.sql           # Flyway schema
src/test/...                          # 14 tests on H2
load/
  load_probe.py                       # stdlib HTTP load probe
  k6-script.js                        # k6 scenario with thresholds
```

## Stack

Java 21, Spring Boot 4.0, Spring Security, Spring Data JPA, Flyway, PostgreSQL (prod) / H2 (test + demo), jjwt 0.12, JUnit 5, Mockito, JaCoCo, Maven (wrapper), Docker.

## License

MIT
