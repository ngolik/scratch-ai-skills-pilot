# Architecture & Design — named-toggle

## 1. Context

- **Problem:** Clients need to set and read a single named boolean "toggle"
  (e.g. feature flag), stored only in process memory, via a small JSON HTTP
  API — analogous in shape to the existing named-counter feature but with a
  different resource and, uniquely among current features, an explicit
  requirement for classic Spring layering instead of a flat package.
- **Goals:** `PUT`/`GET /api/v1/toggles/{name}` with create-or-replace
  semantics, reusing this service's existing `400 validation_failed` and
  `404 not_found` error shapes; code split into
  controller/dto/service/entity/repository/config with enforced dependency
  direction (controller → service → repository).
- **Non-goals:** persistence beyond process memory, auth, list/delete/TTL,
  default-on-missing GET, hexagonal packaging, refactoring `greeting` /
  `farewell` / `counter` into the same layout.
- **Constraints:** stay on Java 21 + Spring Boot 4.1.0; do not overload
  `validation.NameValidationConstants` (person names) for toggle names; reuse
  `greeting.ValidationErrorResponse` / `FieldErrorDetail` — do not invent a
  third error document type; 4096-byte body cap already applies globally.
- **Sources used:** `docs/briefs/named-toggle.md`, `docs/ai-context/system-overview.md`,
  `docs/ai-context/constraints.md`, code areas `counter/*`,
  `greeting/GreetingValidationExceptionHandler.java`.

## 2. Current state (as evidenced)

- `counter/` is the closest analog: flat package, `CounterController` (path-variable
  `@Pattern` name validation via `@Validated`), `CounterService` (single
  `ConcurrentHashMap`, entity and DTO are the same record), a
  package-local `CounterExceptionHandler` (`@RestControllerAdvice(assignableTypes = CounterController.class)`)
  that maps a not-found exception to the shared `ValidationErrorResponse` shape
  with `error: "not_found"`.
- App-wide `greeting.GreetingValidationExceptionHandler` (unscoped
  `@RestControllerAdvice`) already handles, for every controller in the app:
  `MethodArgumentNotValidException` (body `@Valid` failures),
  `HttpMessageNotReadableException` (malformed/non-boolean JSON body),
  `ConstraintViolationException` (path-variable `@Pattern` failures) — all
  mapped to `400 validation_failed` using `greeting.ValidationErrorResponse` /
  `FieldErrorDetail`. Toggle name and body validation reuse this handler
  as-is; no new advice is needed for `400`.
- `CounterNameConstants` sets a precedent: a feature-local name-pattern
  constant, deliberately separate from `validation.NameValidationConstants`.
  Toggle needs its own analogous constant (same regex shape as counters, per
  brief) — not a shared one.
- No existing feature in this repo uses the controller/dto/service/entity/repository/config
  split; `counter` and `greeting`/`farewell` are flat. This is the first
  feature required to use it (brief + `system-overview.md` both anticipate it).

## 3. Recommended architecture

### 3.1 Components and responsibilities

| Component | Responsibility | Notes |
| --- | --- | --- |
| `toggle.controller` | HTTP routes (`PUT`/`GET /api/v1/toggles/{name}`), status codes, path-variable name validation | Accepts/returns DTOs only; depends on `toggle.service` only |
| `toggle.dto` | `SetToggleRequest` (request), `ToggleResponse` (response) | No dependency on entity or repository |
| `toggle.service` | Validate request shape at the use-case level, upsert/get, map entity ↔ dto, raise not-found | Depends on `toggle.entity` + `toggle.repository`; only component that touches both |
| `toggle.entity` | `Toggle` stored model (`name`, `enabled`, `updatedAt`) | Never serialized directly as an HTTP response |
| `toggle.repository` | In-memory save/find-by-name for `Toggle` | No HTTP types, no DTO types, no framework web annotations |
| `toggle.config` | Wires the in-memory repository bean (and any other feature beans) | Only place that instantiates the repository implementation |
| `toggle.controller` (exception mapping) | Maps "toggle not found" to the shared `404 not_found` envelope | Package-scoped advice, same pattern as `counter.CounterExceptionHandler`; reuses `greeting.ValidationErrorResponse` |

### 3.2 Interactions and data flow

- **PUT (create-or-replace), happy path:** controller validates `{name}`
  (path pattern) and binds/validates the JSON body (`enabled` required
  boolean) → service upserts via repository → service maps the returned
  entity to `ToggleResponse` → controller returns `200`.
- **GET, happy path:** controller validates `{name}` → service asks
  repository for the entity → maps to `ToggleResponse` → controller returns
  `200`.
- **GET, unknown name:** repository returns empty → service raises a
  toggle-not-found signal → package-scoped advice returns `404 not_found`
  (same envelope shape as counters).
- **Invalid `{name}` or invalid/missing `enabled`:** caught by the existing
  app-wide `greeting.GreetingValidationExceptionHandler` → `400 validation_failed`.
  No new validation-error component needed.
- **Failure / retry / compensation:** none — single-process in-memory map,
  no external calls, no retries.

### 3.3 Trust boundaries and data

- Data class: process-local, non-sensitive (toggle name + boolean + timestamp).
  No PII, no secrets.
- Authn/authz: none, unchanged for this service.
- Request bodies are untrusted input; validated via Bean Validation on the DTO
  and the existing global body-size filter — no new size check per brief.

## 4. Quality attributes

| Attribute | Target / strategy |
| --- | --- |
| Reliability | Single `ConcurrentHashMap`-backed repository; atomic upsert per key (same pattern as `CounterService.increment`'s `compute`), so concurrent `PUT`s on the same name never interleave-corrupt state |
| Observability | None added beyond existing app defaults — out of scope per brief |
| Performance | O(1) in-memory lookups; no I/O; not a concern at this scale |
| Security | No new attack surface beyond existing validated-input pattern; no stack traces in error bodies (unchanged app behavior) |

## 5. Decisions

| Decision | Options considered | Choice | Rationale |
| --- | --- | --- | --- |
| Package layout | Flat (like `counter/`) vs. layered (`controller`/`dto`/`service`/`entity`/`repository`/`config`) | Layered | Brief explicitly requires classic Spring layers for this feature only; do not retrofit other features |
| Repository wiring | `@Repository`-annotated impl auto-detected vs. explicit `@Bean` in a `config` class | Explicit `@Bean` in `toggle.config` | Brief calls out `config` as the layer that "wires the in-memory repository" — makes the wiring point visible and testable, and keeps `toggle.repository` a plain, framework-annotation-light package |
| Toggle name validation | Reuse `validation.NameValidationConstants` vs. new feature-local constant | New feature-local constant in `toggle.controller` (or an internal validation holder within it) | Brief forbids overloading the person-name constants; mirrors `CounterNameConstants` precedent, one level per feature |
| `400`/`404` error handling | New toggle-specific advice for both vs. reuse existing components | Reuse app-wide `GreetingValidationExceptionHandler` for `400`; add one small package-scoped advice (mirroring `CounterExceptionHandler`) for `404` | Brief requires reusing existing shapes and forbids a third error document type; `400` path is already fully generic today |
| Not-found exception ownership | Define `ToggleNotFoundException` in `toggle.controller` (co-located with its handler, mirroring `counter`'s flat package) vs. in `toggle.service` (co-located with the code that raises it) | `toggle.service` | Pre-push review (2026-08-17) flagged the controller-owned option as a reverse dependency: it forced `toggle.service` to import a type from `toggle.controller`, breaking the brief's required unidirectional `controller → service → repository` direction. Defining it in `service` lets `toggle.controller.ToggleExceptionHandler` import it downward, same as it already imports `toggle.service.ToggleService` |
| Entity vs. DTO for `PUT`/`GET` responses | Return entity directly vs. map to a response DTO | Map to `ToggleResponse` in `toggle.service` | Brief: "entity must not be the HTTP response body" |

## 6. Open questions

None — brief marks open questions as resolved (in-memory only, GET unknown is `404`, no list/delete).

## 7. Implementation implications

- New package tree under `src/main/java/com/example/scratch/toggle/` with the
  six sub-packages above; no changes to `greeting/`, `farewell/`, `counter/`,
  or `validation/`.
- Impl plan must enumerate: DTOs (`SetToggleRequest`, `ToggleResponse`),
  entity (`Toggle`), repository interface + in-memory implementation,
  service, controller, config bean, package-scoped not-found advice, and a
  feature-local name-pattern constant.
- Impl plan must cover the layering/dependency-direction check called out in
  the brief's test requirements (controller does not import repository;
  repository does not import dto) as part of the test suite, alongside the
  WebMvc happy-path/validation/404/regression tests.
- No changes required to `RequestSizeLimitFilter`, `HealthController`, or any
  other existing controller/config.
