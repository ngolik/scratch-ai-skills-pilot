# Implementation Plan — Health Endpoint

## Summary

- **Goal:** Stand up a minimal Spring Boot service exposing `GET /health` → `200`, `text/plain`, body `ok`; document it in README.
- **In scope:** New Maven-based Spring Boot 3.x project, one REST controller, one README section.
- **Out of scope:** Auth, Actuator, dependency/readiness checks, custom port config, CI/deployment wiring, multi-module structure.
- **Architecture input:** `docs/architecture/health-endpoint.md`

## Touch map

| Area | Why |
| --- | --- |
| `pom.xml` (new) | Maven project descriptor; declares Spring Boot 3.x parent + `spring-boot-starter-web` |
| `src/main/java/<base-package>/` — application entry point (new) | `@SpringBootApplication` class to boot the embedded server on port 8080 |
| `src/main/java/<base-package>/` — health controller (new) | `@RestController` handling `GET /health` |
| `src/test/java/<base-package>/` — controller test (new) | Verifies status/content-type/body for `/health` |
| `README.md` | Add "Health check" section per architecture doc §7 |

`[OPEN — engineering step to decide]` Exact base package name (e.g. `com.<org>.health`) — no existing package evidenced in this repo since it currently has no code.

## Steps

### Step 1 — Scaffold the Spring Boot project

- **Outcome:** A buildable, runnable Spring Boot 3.x application with no endpoints yet — `mvn spring-boot:run` starts cleanly on port 8080.
- **Approach:** Add `pom.xml` (Spring Boot 3.x parent, `spring-boot-starter-web`, Java version per Spring Boot 3.x baseline) and a single `@SpringBootApplication` entry-point class. No controllers yet.
- **Tests:** Manual — `mvn spring-boot:run` (or packaged jar) starts without errors and the port is listening.
- **Done when:** App boots successfully and the reviewer can confirm the build/run commands work from a clean checkout.

### Step 2 — Add the `/health` endpoint

- **Outcome:** `GET /health` returns `200`, `Content-Type: text/plain`, body `ok`.
- **Approach:** Add one `@RestController` with a `GET /health` mapping, explicitly producing `text/plain` (do not rely on default content negotiation) and returning the literal string `ok`.
- **Tests:** Unit test (`@WebMvcTest` or equivalent slice test) asserting status `200`, `Content-Type` starts with `text/plain`, and body equals `ok`. Manual: `curl -i localhost:8080/health` cross-check.
- **Done when:** Automated test passes and manual curl matches the architecture doc's §3.2 happy path exactly (status, content-type, body).

### Step 3 — Document the endpoint in README

- **Outcome:** README has a short "Health check" section describing the route, method, and expected response.
- **Approach:** Add a section (not just a one-line bullet, per prior decision) covering: endpoint path, method, response status/content-type/body, and the one-line run command from Step 1.
- **Tests:** Manual review only — confirm the documented behavior matches what Step 2 actually returns.
- **Done when:** A reader unfamiliar with the repo can run the app and hit `/health` using only the README.

## Risks and mitigations

| Risk | Mitigation |
| --- | --- |
| Local machine lacks a compatible JDK/Maven for Spring Boot 3.x (which requires Java 17+) | Confirm `java -version` and `mvn -version` before Step 1; note the required JDK baseline in the README alongside the run command |
| Default Spring content negotiation returns an unexpected `Content-Type` (e.g., adds a charset suffix, or negotiates JSON if an `Accept` header is sent) | Step 2's unit test asserts the exact `Content-Type` and body so any drift is caught immediately, not discovered manually later |
| Port 8080 already in use locally | Not fixed in code (out of scope per architecture decision); document the standard `--server.port=<n>` override as a manual workaround in the README |

## Open questions

- Base package name for the Java sources (see Touch map) — low-stakes, can be resolved at the start of the engineering step rather than blocking planning.
