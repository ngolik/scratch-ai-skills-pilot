# Architecture & Design — Health Endpoint

## 1. Context

- **Problem:** The repo has no running service and no way to check liveness. We need a minimal `/health` endpoint that returns plain text `ok`, documented in the README.
- **Goals:** Stand up the smallest possible HTTP surface that answers `/health` with `200 ok`; note its existence in README.
- **Non-goals:** No auth, no JSON payload, no dependency checks (DB, downstream services), no metrics/tracing, no deployment/CI wiring beyond what already exists.
- **Constraints:** Pilot scope — minimal effort, minimal footprint. Stack fixed to Java + Spring Boot 3.x (latest stable), default port 8080.
- **Sources used:** README.md (repo root); feature brief and stack decision from this conversation.

## 2. Current state (as evidenced)

- The repository contains only `README.md` — no application code, no build manifest, no server process.
- Hosting/deployment target remains out of scope for this pilot — local run via the standard Spring Boot embedded server only.

## 3. Recommended architecture

### 3.1 Components and responsibilities

| Component | Responsibility | Notes |
| --- | --- | --- |
| Spring Boot application (embedded server) | Boots an embedded servlet container on port 8080; hosts the web layer | New — first component in this repo |
| REST controller (web layer) | Handles `GET /health`, returns a static `ok` body as `text/plain` | Single small controller; no service/repository layers needed at this scope |
| README | Documents that `/health` exists and what it returns, under a short "Health check" section | Existing file, gets one new section |

Single application, single controller is sufficient at this scope — no service layer, no persistence, no Actuator.

### 3.2 Interactions and data flow

- Happy path: client sends `GET /health` → embedded server routes to the controller → responds `200 OK`, `Content-Type: text/plain`, body `ok`.
- Failure/retry/compensation: not applicable — no external dependencies to fail against. Any request error (wrong method/path) can fall through to the runtime's default handling; no custom error design needed at this scope.

### 3.3 Trust boundaries and data

- Data classes: none — no user input is accepted, no data is returned beyond the literal string `ok`.
- Authn/authz: none. Endpoint is intentionally unauthenticated, consistent with typical health-check conventions (load balancers / orchestrators probe it without credentials).

## 4. Quality attributes

| Attribute | Target / strategy |
| --- | --- |
| Reliability | Endpoint has no dependencies, so it cannot fail independently of the process itself being up |
| Observability | None added at this scope — the endpoint's response *is* the signal; no logging/metrics required for a pilot |
| Performance | Not a concern at this scope — single static response, no I/O |
| Security | No secrets, no input handling, no auth surface introduced |

## 5. Decisions

| Decision | Options considered | Choice | Rationale |
| --- | --- | --- | --- |
| Runtime/stack | Node, Python, Go, Java + Spring Boot | Java + Spring Boot 3.x (latest stable) | Fixed by stakeholder decision |
| Build tool | Maven vs. Gradle | Maven | Delegated choice; Maven is the more conventional default for this kind of pilot and keeps tooling/CI expectations predictable — no functional difference at this scope |
| Health mechanism | Spring Boot Actuator `/actuator/health` vs. a plain `@RestController` | Plain `@RestController` | Actuator's default response is JSON (`{"status":"UP"}`); reshaping it to exactly `text/plain "ok"` means fighting the framework, so a small dedicated controller is simpler and matches the brief exactly |
| Response format | Plain text vs. JSON `{"status":"ok"}` | Plain text `ok` | Matches brief explicitly; avoids adding a JSON dependency for a pilot |
| Endpoint scope | Single `/health` vs. `/health` + `/ready` (liveness vs. readiness split) | Single `/health` | Brief asks for one endpoint; readiness split has no dependencies to check yet, so it would add a distinction without a difference |

## 6. Open questions

- None currently blocking. Hosting/deployment target stays undefined but is explicitly out of scope for this pilot.

## 7. Implementation implications

- Standard Spring Boot Maven project layout (build manifest + a single application module); no multi-module split needed at this scope.
- A web layer containing one small REST controller for `GET /health`, configured to return `text/plain` explicitly (not the framework's default JSON negotiation).
- Default embedded server port (8080); no custom port configuration needed.
- Add a "Health check" section to `README.md` documenting the route, method, and expected response.
- No test scaffolding, CI, or deployment config is implied by this scope — leave those out unless requested separately.
