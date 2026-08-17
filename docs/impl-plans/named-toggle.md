# Implementation Plan — named-toggle

## Summary

- **Goal:** Add `PUT`/`GET /api/v1/toggles/{name}` for a named in-memory
  boolean toggle, using classic Spring layers (controller/dto/service/entity/repository/config),
  reusing existing `400 validation_failed` / `404 not_found` shapes.
- **In scope:** new `toggle` package tree, its tests, a brief README update.
- **Out of scope:** persistence, auth, list/delete/TTL, changes to `greeting`/`farewell`/`counter`/`validation`.
- **Architecture input:** `docs/architecture/named-toggle.md`

## Touch map

| Area | Why |
| --- | --- |
| `src/main/java/com/example/scratch/toggle/entity/` (new) | Stored `Toggle` model |
| `src/main/java/com/example/scratch/toggle/repository/` (new) | In-memory save/find-by-name |
| `src/main/java/com/example/scratch/toggle/config/` (new) | Wires the repository bean |
| `src/main/java/com/example/scratch/toggle/dto/` (new) | `SetToggleRequest`, `ToggleResponse` |
| `src/main/java/com/example/scratch/toggle/service/` (new) | Upsert/get use cases, entity↔dto mapping |
| `src/main/java/com/example/scratch/toggle/controller/` (new) | `PUT`/`GET` routes, name validation, `404` advice |
| `src/test/java/com/example/scratch/toggle/` (new) | Unit + WebMvc tests, layering check |
| `README.md` | Document new endpoints |

Reference-only (no changes): `counter/*` (pattern to mirror), `greeting/GreetingValidationExceptionHandler.java`,
`greeting/ValidationErrorResponse.java`, `greeting/FieldErrorDetail.java` (reused, not modified).

## Steps

### Step 1 — Entity, repository, config wiring

- **Outcome:** A `Toggle` entity and an in-memory repository exist and are
  wired as a Spring bean, independent of any HTTP concern.
- **Approach:**
  - `toggle.entity.Toggle` — plain class/record: `name`, `enabled`, `updatedAt` (`Instant`).
  - `toggle.repository.ToggleRepository` — interface: `save(Toggle)`, `findByName(String)` returning `Optional<Toggle>`.
  - `toggle.repository.InMemoryToggleRepository` — `ConcurrentHashMap`-backed implementation (plain class, no `@Repository` annotation — wiring happens in `config`).
  - `toggle.config.ToggleConfig` — `@Configuration` class with `@Bean ToggleRepository toggleRepository()` returning `new InMemoryToggleRepository()`.
- **Tests:** Unit test for `InMemoryToggleRepository`: save-then-find returns the same values; find-unknown returns empty; save-twice-same-name overwrites (upsert semantics).
- **Done when:** Repository unit tests pass; `mvn -q compile` succeeds.

### Step 2 — DTOs and name validation constant

- **Outcome:** Request/response shapes and the toggle name pattern exist, matching the brief's JSON contract and regex.
- **Approach:**
  - `toggle.dto.SetToggleRequest` — record with `Boolean enabled` (`@NotNull`, so missing/non-boolean fails Bean Validation → existing app-wide `400` handler).
  - `toggle.dto.ToggleResponse` — record: `name`, `enabled`, `updatedAt`.
  - A feature-local name-pattern constant (e.g. `toggle.controller.ToggleNameConstants`, mirroring `counter.CounterNameConstants`) holding `^(?!.*--)[a-z0-9]([a-z0-9-]{0,38}[a-z0-9])?$` — do **not** reuse `validation.NameValidationConstants` or `counter.CounterNameConstants`.
- **Tests:** None standalone (records/constants); covered by Step 4 WebMvc tests.
- **Done when:** Types compile; reviewed against brief's JSON shapes.

### Step 3 — Service layer

- **Outcome:** `toggle.service.ToggleService` implements upsert-or-create and get, mapping `Toggle` ↔ `ToggleResponse`, and signals not-found on a missing `GET`.
- **Approach:**
  - `setToggle(String name, boolean enabled)`: build/update a `Toggle` with `Instant.now()`, `repository.save(...)`, return `ToggleResponse`.
  - `getToggle(String name)`: `repository.findByName(...)`, map to `ToggleResponse` or throw `toggle.service.ToggleNotFoundException` — defined in `toggle.service` (not `toggle.controller`) so the exception type flows downward with the rest of the layer's outputs; see architecture doc §5 "Not-found exception ownership" (patched 2026-08-17 after pre-push review).
  - Depends on `toggle.repository` + `toggle.entity`; does **not** depend on `toggle.dto` for anything but the return mapping (dto is the boundary type, not leaked into repository/entity).
- **Tests:** Unit tests for `ToggleService` (with a real `InMemoryToggleRepository`, no mocking needed given its simplicity): create-then-get returns matching `enabled`; overwrite `true`→`false` reflected on next get; get-unknown raises the not-found signal.
- **Done when:** Service unit tests pass.

### Step 4 — Controller + not-found mapping (vertical slice complete)

- **Outcome:** `PUT`/`GET /api/v1/toggles/{name}` are live end-to-end with `200`/`404` behavior.
- **Approach:**
  - `toggle.controller.ToggleController`, `@RestController @Validated`, mirroring `CounterController`'s `@PathVariable @Pattern` name validation (same reasoning: routes path-variable violations through `ConstraintViolationException`, caught by the existing app-wide handler).
    - `@PutMapping("/api/v1/toggles/{name}")` — `@RequestBody @Valid SetToggleRequest` → `service.setToggle(...)` → `200`.
    - `@GetMapping("/api/v1/toggles/{name}")` → `service.getToggle(...)` → `200`, or propagate not-found.
    - Controller imports `toggle.service` and `toggle.dto` only — **not** `toggle.repository` or `toggle.entity`.
  - `toggle.controller.ToggleExceptionHandler` — `@RestControllerAdvice(assignableTypes = ToggleController.class)`, mirroring `CounterExceptionHandler`: catch the not-found exception, return `greeting.ValidationErrorResponse("not_found", [FieldErrorDetail("name", "toggle does not exist")])` with `404`.
- **Tests:** WebMvc tests (`@WebMvcTest(ToggleController.class)` or full `@SpringBootTest` + `MockMvc`, matching existing test style):
  - `PUT` create → `GET` → same `enabled`.
  - `PUT` overwrite `true` → `false` → `GET` reflects `false`.
  - `GET` unknown name → `404` with the `not_found` envelope.
- **Done when:** WebMvc tests above pass.

### Step 5 — Validation, layering, and regression coverage

- **Outcome:** Full brief test-requirements list is covered.
- **Approach:** Add/confirm tests for:
  - Invalid `{name}` (bad chars, too long, leading/trailing hyphen, `--`) → `400 validation_failed`, field `"name"`.
  - Missing/non-boolean `enabled` → `400 validation_failed`, field `"enabled"`.
  - A layering test: read `ToggleController.java` and `InMemoryToggleRepository.java` (or equivalent) source text and assert the controller's imports do not include `toggle.repository`, and the repository package's source does not import `toggle.dto` — plain source-text assertion (no ArchUnit dependency in this repo; do not add one).
  - Regression checks: `GET /health`, `POST /api/v1/greetings`, `POST /api/v1/farewells`, counter increment+get still return their existing contracts unchanged (smoke-level WebMvc tests, or confirm existing tests still pass if already covering this).
- **Tests:** As listed above.
- **Done when:** `mvn test` passes for the full suite, including the new toggle tests and all pre-existing tests.

### Step 6 — README update

- **Outcome:** `README.md` briefly documents the new endpoints.
- **Approach:** Add `PUT`/`GET /api/v1/toggles/{name}` next to the existing endpoint list, with method, path, and one example request/response — same level of detail as the existing counter entry.
- **Tests:** None (docs only).
- **Done when:** README change reviewed for consistency with existing entries.

## Risks and mitigations

| Risk | Mitigation |
| --- | --- |
| Path-variable `@Pattern` validation silently falls back to Spring's native `HandlerMethodValidationException` instead of `ConstraintViolationException` (breaking the `400` shape) | Follow `CounterController`'s exact pattern: class-level `@Validated` + `@PathVariable @Pattern`, verified by the existing app-wide handler's `ConstraintViolationException` branch; add a name-validation test to catch a regression immediately |
| Not-found exception/handler split across packages could either break the "controller does not import repository" rule, or (as first drafted) create a reverse `service → controller` dependency | Keep the not-found exception in `toggle.service` (thrown where it's raised) and import it into `toggle.controller.ToggleExceptionHandler`, which already depends on `toggle.service` — preserves the brief's `controller → service → repository` direction with no repository import in controller and no controller import in service |
| Config-based bean wiring (`toggle.config.ToggleConfig`) not picked up if component scanning is restricted | Confirm `@SpringBootApplication` base package covers `com.example.scratch` (already true for existing `counter`/`greeting` `@Service`/`@RestController` beans); no separate `@ComponentScan` needed since `@Configuration` classes are picked up the same way |

## Open questions

None — brief and architecture doc left no open items.
