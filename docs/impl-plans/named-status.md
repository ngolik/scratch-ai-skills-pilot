# Implementation Plan — named-status

## Summary

- **Goal:** Add `PUT`/`GET /api/v1/statuses/{name}` for a named in-memory
  status message, using classic Spring layers
  (controller/dto/service/entity/repository/config), reusing existing
  `400 validation_failed` / `404 not_found` shapes.
- **In scope:** new `status` package tree, its tests, a brief README update.
- **Out of scope:** persistence, auth, list/delete/TTL, changes to
  `greeting`/`farewell`/`counter`/`notes`/`toggle`/`validation`.
- **Architecture input:** `docs/architecture/named-status.md`

## Touch map

| Area | Why |
| --- | --- |
| `src/main/java/com/example/scratch/status/entity/` (new) | Stored `Status` model |
| `src/main/java/com/example/scratch/status/repository/` (new) | In-memory save/find-by-name |
| `src/main/java/com/example/scratch/status/config/` (new) | Wires the repository bean |
| `src/main/java/com/example/scratch/status/dto/` (new) | `SetStatusRequest`, `StatusResponse` |
| `src/main/java/com/example/scratch/status/service/` (new) | Upsert/get use cases, trim/blank/max-length `message` validation, entity↔dto mapping |
| `src/main/java/com/example/scratch/status/controller/` (new) | `PUT`/`GET` routes, name validation, `404` + message-invalid `400` advice |
| `src/test/java/com/example/scratch/status/` (new) | Unit + WebMvc tests, layering check |
| `README.md` | Document new endpoints |

Reference-only (no changes): `toggle/*` (layered-package pattern to mirror),
`greeting/GreetingValidationExceptionHandler.java`,
`greeting/ValidationErrorResponse.java`, `greeting/FieldErrorDetail.java`
(reused, not modified), `notes/domain/NoteText.java` (trim/blank/max-length
invariant precedent — logic mirrored in `status.service`, not an entity
constructor; see architecture doc §5).

## Steps

### Step 1 — Entity, repository, config wiring

- **Outcome:** A `Status` entity and an in-memory repository exist and are
  wired as a Spring bean, independent of any HTTP concern.
- **Approach:**
  - `status.entity.Status` — record: `name`, `message`, `updatedAt` (`Instant`).
  - `status.repository.StatusRepository` — interface: `save(Status)`, `findByName(String)` returning `Optional<Status>`.
  - `status.repository.InMemoryStatusRepository` — `ConcurrentHashMap`-backed implementation (plain class, no `@Repository` annotation — wiring happens in `config`).
  - `status.config.StatusConfig` — `@Configuration` class with `@Bean StatusRepository statusRepository()` returning `new InMemoryStatusRepository()`.
- **Tests:** Unit test for `InMemoryStatusRepository`: save-then-find returns the same values; find-unknown returns empty; save-twice-same-name overwrites (upsert semantics).
- **Done when:** Repository unit tests pass; `mvn -q compile` succeeds.

### Step 2 — DTOs and name validation constant

- **Outcome:** Request/response shapes and the status name pattern exist, matching the brief's JSON contract and regex.
- **Approach:**
  - `status.dto.SetStatusRequest` — record with `String message` (`@NotNull(message = "must not be blank")` — catches missing/null; missing-field JSON binds to `null`, fails Bean Validation → existing app-wide `400` handler, field `"message"`).
  - `status.dto.StatusResponse` — record: `name`, `message`, `updatedAt`.
  - A feature-local name-pattern constant `status.controller.StatusNameConstants` (mirroring `toggle.controller.ToggleNameConstants`) holding `^(?!.*--)[a-z0-9]([a-z0-9-]{0,38}[a-z0-9])?$` (brief: name rules match counters/toggles) — do **not** reuse `validation.NameValidationConstants`, `counter.CounterNameConstants`, or `toggle.controller.ToggleNameConstants`.
- **Tests:** None standalone (records/constants); covered by Step 4/5 WebMvc tests.
- **Done when:** Types compile; reviewed against brief's JSON shapes.

### Step 3 — Service layer (message trim/blank/max-length invariant lives here)

- **Outcome:** `status.service.StatusService` implements upsert-or-create and get, mapping `Status` ↔ `StatusResponse`, trims and validates `message` content, and signals not-found on a missing `GET`.
- **Approach:**
  - `setStatus(String name, String rawMessage)`:
    - trim `rawMessage`; if the trimmed value is empty → throw `status.service.StatusMessageInvalidException("must not be blank")`.
    - if trimmed length > 80 → throw `StatusMessageInvalidException("must be at most 80 characters")`.
    - build/update a `Status` with the **trimmed** message and `Instant.now()`, `repository.save(...)`, return `StatusResponse` (trimmed message).
  - `getStatus(String name)`: `repository.findByName(...)`, map to `StatusResponse` or throw `status.service.StatusNotFoundException` — defined in `status.service` (not `status.controller`), matching the architecture doc's dependency-direction rationale (same fix already established for `named-toggle`).
  - Both exceptions live in `status.service`, imported downward by `status.controller`'s advice — controller never imports repository/entity, service never imports controller.
  - Depends on `status.repository` + `status.entity`; does **not** depend on `status.dto` for anything but the return mapping.
- **Tests:** Unit tests for `StatusService` (real `InMemoryStatusRepository`, no mocking needed): create-then-get returns the same trimmed `message`; overwrite reflects the new trimmed `message` on next get; get-unknown raises the not-found signal; " away " → stored/returned as `"away"`; blank (`"   "`) raises `StatusMessageInvalidException`; 81-char trimmed message raises `StatusMessageInvalidException`; 82 raw chars that trim to 80 succeeds (raw-vs-trimmed-length distinction from architecture doc §2).
- **Done when:** Service unit tests pass, including the trim-boundary cases above.

### Step 4 — Controller + exception mapping (vertical slice complete)

- **Outcome:** `PUT`/`GET /api/v1/statuses/{name}` are live end-to-end with `200`/`400`/`404` behavior.
- **Approach:**
  - `status.controller.StatusController`, `@RestController @Validated`, mirroring `ToggleController`'s `@PathVariable @Pattern` name validation (routes path-variable violations through `ConstraintViolationException`, caught by the existing app-wide handler).
    - `@PutMapping("/api/v1/statuses/{name}")` — `@RequestBody @Valid SetStatusRequest` → `service.setStatus(...)` → `200`.
    - `@GetMapping("/api/v1/statuses/{name}")` → `service.getStatus(...)` → `200`, or propagate not-found.
    - Controller imports `status.service` and `status.dto` only — **not** `status.repository` or `status.entity`.
  - `status.controller.StatusExceptionHandler` — `@RestControllerAdvice(assignableTypes = StatusController.class)`, mirroring `ToggleExceptionHandler`, with **two** handlers:
    - `StatusNotFoundException` → `404`, `greeting.ValidationErrorResponse("not_found", [FieldErrorDetail("name", "status does not exist")])`.
    - `StatusMessageInvalidException` → `400`, `greeting.ValidationErrorResponse("validation_failed", [FieldErrorDetail("message", exception.getMessage())])`.
- **Tests:** WebMvc tests (`@WebMvcTest` or full `@SpringBootTest` + `MockMvc`, matching existing test style):
  - `PUT` create → `GET` → same `message`.
  - `PUT` overwrite → `GET` reflects the new trimmed `message`.
  - `GET` unknown name → `404` with the `not_found` envelope.
- **Done when:** WebMvc tests above pass.

### Step 5 — Validation, layering, and regression coverage

- **Outcome:** Full brief test-requirements list is covered.
- **Approach:** Add/confirm tests for:
  - Invalid `{name}` (bad chars, too long, leading/trailing hyphen, `--`) → `400 validation_failed`, field `"name"`.
  - Missing `message` → `400 validation_failed`, field `"message"` (via `@NotNull`).
  - Blank `message` (whitespace-only) → `400 validation_failed`, field `"message"` (via `StatusMessageInvalidException`).
  - Too-long `message` (trimmed length > 80) → `400 validation_failed`, field `"message"`.
  - Both `400` paths (DTO-level missing vs. service-level blank/too-long) must produce the **identical** envelope shape — assert this explicitly, since they come from two different exception handlers.
  - A layering test: read `StatusController.java` and `InMemoryStatusRepository.java` (or equivalent) source text and assert the controller's imports do not include `status.repository`, and the repository package's source does not import `status.dto` — plain source-text assertion (no ArchUnit dependency in this repo; do not add one).
  - Regression checks: `GET /health`, `POST /api/v1/greetings`, `POST /api/v1/farewells`, counter increment+get, notes create+get, toggle put+get still return their existing contracts unchanged (smoke-level WebMvc tests, or confirm existing tests still pass if already covering this).
- **Tests:** As listed above.
- **Done when:** `mvn test` passes for the full suite, including the new status tests and all pre-existing tests.

### Step 6 — README update

- **Outcome:** `README.md` briefly documents the new endpoints.
- **Approach:** Add `PUT`/`GET /api/v1/statuses/{name}` next to the existing endpoint list, with method, path, and one example request/response — same level of detail as the existing counter/toggle entries.
- **Tests:** None (docs only).
- **Done when:** README change reviewed for consistency with existing entries.

## Risks and mitigations

| Risk | Mitigation |
| --- | --- |
| Path-variable `@Pattern` validation silently falls back to Spring's native `HandlerMethodValidationException` instead of `ConstraintViolationException` (breaking the `400` shape) | Follow `ToggleController`'s exact pattern: class-level `@Validated` + `@PathVariable @Pattern`, verified by the existing app-wide handler's `ConstraintViolationException` branch; add a name-validation test to catch a regression immediately |
| Two independent `400` code paths for `message` (DTO `@NotNull` for missing vs. service-raised `StatusMessageInvalidException` for blank/too-long) drift into different JSON shapes over time | Both funnel through `greeting.ValidationErrorResponse`/`FieldErrorDetail` with field `"message"`; Step 5 adds an explicit test asserting both produce the same envelope shape |
| `@Size`-style raw-length validation accidentally applied to `message` on the DTO, silently rejecting valid inputs that are long-but-trim-short (or the reverse) | Do **not** add any `@Size`/`@Length` annotation to `SetStatusRequest.message`; length is checked only after trim, only in `status.service` (architecture doc §2, §5) |
| Not-found / message-invalid exceptions defined in the wrong package could create a reverse `service → controller` dependency or break "controller does not import repository" | Keep both exceptions in `status.service` (thrown where raised) and import them into `status.controller.StatusExceptionHandler`, which already depends on `status.service` — preserves `controller → service → repository`; layering test in Step 5 catches a regression |
| Config-based bean wiring (`status.config.StatusConfig`) not picked up if component scanning is restricted | Confirm `@SpringBootApplication` base package covers `com.example.scratch` (already true for `toggle`/`counter`/`greeting` beans); no separate `@ComponentScan` needed |

## Open questions

None — brief and architecture doc left no open items.
