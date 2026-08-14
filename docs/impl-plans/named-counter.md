# Implementation Plan — Named Counter API

## Summary

- **Goal:** Add `POST /api/v1/counters/{name}/increments` (create-or-increment)
  and `GET /api/v1/counters/{name}` (read), backed by an in-process,
  thread-safe store, reusing the existing `400 validation_failed` envelope for
  an invalid name and adding a new `404 not_found` envelope for an unknown
  counter — without changing `/health`, greetings, or farewells.
- **In scope:** New `counter` package (controller, service, store, response
  DTO), a package-local name-slug constant, one new handler on the existing
  shared advice (`ConstraintViolationException` → `400`), a new
  counter-scoped advice (`404`), unit/WebMvc tests, README update.
- **Out of scope:** Persistence, auth, decrement/reset/list-all/TTL/cap,
  arbitrary-delta increment, rate limiting, OpenAPI, a second request-size
  check (per architecture doc §1). No change to `NameValidationConstants`.
- **Architecture input:** `docs/architecture/named-counter.md`

## Touch map

| Area | Why |
| --- | --- |
| `src/main/java/com/example/scratch/counter/` (new package) | Controller, service, in-memory store, response DTO, counter-name constant, `404` advice |
| `src/main/java/com/example/scratch/greeting/GreetingValidationExceptionHandler.java` | Add one new `@ExceptionHandler(ConstraintViolationException.class)` (architecture §5 — generic `400` concern, extends the existing shared advice) |
| `src/test/java/com/example/scratch/counter/` (new package) | Unit tests for the store/service (incl. concurrency) and WebMvc tests for both endpoints |
| `src/test/java/com/example/scratch/greeting/`, `farewell/`, `HealthControllerTest` | No code changes; re-run as regression evidence |
| `README.md` | Add a brief new section (methods, paths, example) |

No changes to `pom.xml` (`spring-boot-starter-validation` already present) and
no changes to `NameValidationConstants` or any `greeting`/`farewell` request
DTO.

## Steps

### Step 1 — In-memory store and service (no HTTP yet)

- **Outcome:** `CounterService` can create-or-increment and read a counter by
  name, with no lost updates under concurrent increments on the same name,
  and signals "not found" for a GET-style read of an unknown name. No
  controller yet.
- **Approach:**
  - A small immutable holder for `(value, updatedAt)` per counter name.
  - `CounterService` backed by `ConcurrentHashMap<String, ...>`;
    `increment(name)` uses one atomic per-key update (`compute`/`merge`) that
    creates at `0` then adds `1`, stamping `Instant.now()`, and returns the
    new state; `get(name)` returns the current state or throws an unchecked
    "not found" signal (e.g. `CounterNotFoundException`) when absent.
  - No `@PathVariable`/validation/HTTP concerns here — pure service logic.
- **Tests:** Direct unit tests against `CounterService` (no Spring context):
  first `increment("jobs")` → value `1`; second `increment("jobs")` → value
  `2`; `get` after increment returns the same value; `get` on an unknown name
  throws the not-found signal; a concurrency test that fires N increments
  (e.g. 50, via an `ExecutorService`/`CountDownLatch` fan-out) at the same
  name from multiple threads and asserts the final value equals N exactly
  (no lost updates).
- **Done when:** All the above pass with no Spring context required (fast
  unit tests).

### Step 2 — Increment endpoint, path validation, and the shared `400` handler

- **Outcome:** `POST /api/v1/counters/{name}/increments` returns `200` with
  `{ name, value, updatedAt }` for a valid name (body ignored/absent), and
  `400` with the standard `validation_failed` envelope (field `"name"`) for
  an invalid one.
- **Approach:**
  - Package-local slug constant/pattern (`^[a-z0-9-]{1,40}$`, no
    leading/trailing hyphen, no `--`) — not added to `NameValidationConstants`
    (architecture §5).
  - `CounterController`: `@RestController @Validated`; increment handler
    takes `@PathVariable @Pattern(regexp = ...) String name` (no
    `@RequestBody`); delegates to `CounterService.increment`; maps the
    result to a small response DTO (`name`, `value`, `updatedAt`).
  - Add `@ExceptionHandler(ConstraintViolationException.class)` to
    `GreetingValidationExceptionHandler`, mapping each violation to the
    `FieldErrorDetail` shape using the **leaf** property-path segment (e.g.
    `"name"`, not `"incrementCounter.name"`) as `field`.
- **Tests:** `@WebMvcTest(CounterController.class)`: increment on a new name
  → `200`, `value: 1`; increment again on the same name → `200`, `value: 2`;
  invalid names (uppercase, leading hyphen, trailing hyphen, `--`,
  41-char name, empty segment if reachable) → `400`,
  `error: "validation_failed"`, `details[0].field == "name"`.
- **Done when:** All listed cases pass; existing `GreetingControllerTest` /
  `FarewellControllerTest` still pass unmodified (proves the shared advice
  extension didn't regress the handlers it already carries).

### Step 3 — Read endpoint and the `404` handler

- **Outcome:** `GET /api/v1/counters/{name}` returns `200` with the current
  state for an existing counter and `404` with the `not_found` envelope for
  an unknown one.
- **Approach:**
  - Add the GET handler to `CounterController`, same path-variable
    validation as the increment handler, delegating to `CounterService.get`.
  - New `@RestControllerAdvice(assignableTypes = CounterController.class)` in
    the `counter` package, mapping the not-found signal to `404` with
    `{ error: "not_found", details: [{ field: "name", message: "counter does not exist" }] }`,
    reusing the existing `ValidationErrorResponse`/`FieldErrorDetail` record
    shapes from `greeting`.
- **Tests:** `@WebMvcTest(CounterController.class)`: GET after one increment
  → `200`, `value: 1`; GET after two increments → `200`, `value: 2`; GET on a
  never-incremented but validly-shaped name → `404`,
  `error: "not_found"`, `details[0].field == "name"`; GET with an invalid
  name → `400` (same shape as Step 2, proving both handlers share the
  validation path).
- **Done when:** All listed cases pass; `details[0]` shape for both `400` and
  `404` responses matches the standard envelope exactly.

### Step 4 — Regression pass and README

- **Outcome:** Full suite green; README documents the new endpoints.
- **Approach:**
  - Run the full `mvn test` suite (not just the new package) to confirm
    `/health`, greetings, and farewells are unaffected by the shared-advice
    change from Step 2.
  - Add a short "Counter API" README section (both methods/paths, one
    example increment + one example GET, and the `404` example) near the
    existing greeting/farewell sections.
- **Tests:** Full `mvn test` run (regression evidence for G3). README is
  manual-review only.
- **Done when:** Full suite passes; a reader unfamiliar with the repo can
  `curl` both new endpoints using only the README.

## Risks and mitigations

| Risk | Mitigation |
| --- | --- |
| `ConstraintViolationException`'s property path format varies by Spring/Hibernate Validator version, breaking the "leaf segment → field" extraction | Step 2 tests assert `details[0].field == "name"` exactly; if the extraction is wrong, the test fails loudly before this reaches review, not silently in production |
| Extending the shared `GreetingValidationExceptionHandler` regresses its existing greeting/farewell handlers | Step 2's "done when" explicitly re-runs `GreetingControllerTest`/`FarewellControllerTest` unmodified as regression evidence |
| Atomic `compute`-based increment is subtly wrong under contention (e.g. non-atomic read-modify-write slipping in) | Step 1 includes a dedicated multi-threaded fan-out test asserting the exact final count — this is the architecture's key correctness bet and is tested before any HTTP wiring exists |
| Unbounded store growth (any valid slug creates an entry, no cap) | Accepted per architecture §3.3/§6 — no mitigation in this change; out of scope |

## Open questions

- None blocking.
