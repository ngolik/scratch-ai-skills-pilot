# Architecture & Design — Named Counter API

## 1. Context

- **Problem:** The service exposes `GET /health`, `POST /api/v1/greetings`, and
  `POST /api/v1/farewells`. We need a third, structurally different feature: a
  named, in-process integer counter that a client can increment
  (`POST /api/v1/counters/{name}/increments`) and read
  (`GET /api/v1/counters/{name}`). Unlike greetings/farewells, the "input" is a
  **path segment**, not a JSON body, and the feature is stateful (in-memory)
  rather than a pure function of its input.
- **Goals:** Add create-or-increment and read endpoints with no lost updates
  under concurrent increments on the same name; validate `{name}` against a
  strict slug pattern, reusing the existing `400 validation_failed` envelope;
  add a new `404` envelope (same shape, different `error` code) for unknown
  counters on GET.
- **Non-goals:** Persistence, auth, decrement/reset/list-all/TTL/max-value cap,
  arbitrary-delta increment, rate limiting, OpenAPI/Swagger, any change to
  `/health`, greetings, or farewells, a second request-size check (the
  existing `RequestSizeLimitFilter` already covers this path).
- **Constraints:** Stay on Java 21 + Spring Boot `4.1.0` (already the parent);
  thin controller + small service; the in-memory store lives behind the
  service, not the controller; the counter-name validation pattern is
  **not** a person name and must not be folded into
  `NameValidationConstants`.
- **Sources used:** `docs/briefs/named-counter.md`; existing code (`greeting/`
  and `farewell/` packages, `RequestSizeLimitFilter`,
  `NameValidationConstants`); `docs/architecture/echo-greeting.md` and
  `docs/architecture/echo-farewell.md` (prior decisions and precedent in this
  repo).

## 2. Current state (as evidenced)

- Both existing features follow the same shape: thin `@RestController` →
  small `@Service`, with request validation and the `400` error envelope
  handled entirely by Bean Validation (`@Valid @RequestBody`) plus a single
  **app-wide, unscoped** `@RestControllerAdvice`
  (`greeting.GreetingValidationExceptionHandler`). That advice already
  handles `MethodArgumentNotValidException`, `HttpMessageNotReadableException`,
  and — since the farewell hardening pass — a generic `IllegalArgumentException`
  fallback. It is intentionally not scoped to any one controller; farewell
  already depends on it from a sibling package with no import, only Spring's
  component scan, and that dependency is documented in-code on both sides.
- This is a structurally new shape for the repo: the first path-variable
  input (vs. request body), the first read endpoint on an existing resource,
  and the first mutable, shared, in-process state. None of the current
  request DTOs or exception handlers deal with `@PathVariable` constraint
  violations (`ConstraintViolationException`) or a domain-level "not found."
- `spring-boot-starter-validation` is already a dependency, so `@Validated` +
  `@Pattern` on a path variable requires no new dependency.
- `RequestSizeLimitFilter` is registered with no path restriction, so it
  already applies to both new counter endpoints without any change.

## 3. Recommended architecture

### 3.1 Components and responsibilities

| Component | Responsibility | Notes |
| --- | --- | --- |
| Counter controller (web layer) | Accepts `POST /api/v1/counters/{name}/increments` and `GET /api/v1/counters/{name}`, triggers `{name}` validation, delegates to the service, maps the result to the success JSON shape | New; thin — no store or regex logic here |
| Counter service | Owns the in-memory store; performs atomic create-or-increment and lookup; raises a "not found" signal for GET on an unknown name | New; the only component holding mutable state |
| In-memory store | Maps counter name → (value, last-updated timestamp) | Lives inside the service (per brief constraint), not a separate public component |
| Counter-name validation | Slug pattern (`^[a-z0-9-]{1,40}$`, no leading/trailing/double hyphen), enforced declaratively on the `{name}` path variable | New, package-local constant — deliberately **not** added to `NameValidationConstants` (different rule set, different feature) |
| Validation error mapping (`400`) | Converts path-variable constraint violations into the existing `validation_failed` envelope | **Extends** the existing shared, app-wide advice with one new handler for `ConstraintViolationException`; no new class |
| Not-found error mapping (`404`) | Converts "unknown counter" into a `not_found` envelope with the same shape | New, small advice scoped to the counter controller only — this is genuinely counter-specific, unlike the `400` shape |
| `GreetingController`/`FarewellController`/`HealthController` and their services | Unchanged | Existing |

### 3.2 Interactions and data flow

- **Increment happy path:** Client sends `POST /api/v1/counters/{name}/increments`
  (body ignored/absent) → path-variable validation runs before the method body
  → controller calls the service → service atomically creates the counter at
  `0` if absent and adds `1` in one step (no read-then-write race), stamping
  the current time → controller returns `200` with `{ name, value, updatedAt }`.
- **Get happy path:** Client sends `GET /api/v1/counters/{name}` → path
  validation runs → service looks up the name → found → controller returns
  `200` with the same shape; the timestamp reflects the last increment, not
  the read.
- **Get, unknown name:** Service signals "not found" → the counter-scoped
  advice maps it to `404` with
  `{ error: "not_found", details: [{ field: "name", message: "counter does not exist" }] }`.
- **Invalid `{name}` (either endpoint):** Bean Validation rejects the path
  variable before the controller method runs → the shared advice's new
  handler maps the violation to `400` with the standard `validation_failed`
  envelope, field `"name"`.
- **Wrong HTTP method:** Falls through to Spring's default handling — no
  custom design, per the brief.
- **Oversized body:** Already intercepted by `RequestSizeLimitFilter` ahead of
  routing; no new work.
- **Concurrency:** Two increments racing on the same name must both be
  observed (`1` then `2`, in either wall-clock order) — see §5 for the
  chosen mechanism.

### 3.3 Trust boundaries and data

- **Data classes:** Counter name (caller-supplied, constrained to a narrow
  slug alphabet) and an integer value/timestamp derived entirely from server
  state — no sensitive data, no persistence, lost on restart by design.
- **Authn/authz:** None, consistent with the existing unauthenticated
  endpoints and the brief's non-goals.
- **Input handling:** The `{name}` slug pattern is the only external input;
  it is bounded (1–40 chars, narrow character class) and validated before it
  reaches the service, store key, or response body.
- **Resource exhaustion:** Because any valid slug creates a new in-memory
  entry with no cap, count, or eviction (list-all/reset explicitly
  out of scope), an unauthenticated caller can grow the store unboundedly by
  incrementing many distinct names. This is not merely "the same no-auth
  posture again": `/health`, greetings, and farewells are all stateless pure
  functions, so unauthenticated access there only costs per-request compute.
  This endpoint is the first one where unauthenticated access also buys
  permanent server memory — a qualitatively different risk. Accepted here as
  an explicit trade-off per the brief's non-goals (no cap/TTL, no rate
  limiting), not a gap — but flagged precisely so a follow-up (max-entries
  cap + eviction, or a rate limiter) is tracked before this pilot is exposed
  beyond a trusted/internal environment.

## 4. Quality attributes

| Attribute | Target / strategy |
| --- | --- |
| Reliability | No external dependencies; store is process-local and intentionally volatile, matching the brief |
| Concurrency correctness | Atomic per-key create-or-increment (no lost updates), no explicit locking |
| Observability | None added at this scope, matching existing precedent |
| Performance | O(1) in-memory lookup/update per request; no I/O |
| Security | Same input allow-list posture as greetings/farewells; unbounded store growth accepted per non-goals (see §3.3) |

## 5. Decisions

| Decision | Options considered | Choice | Rationale |
| --- | --- | --- | --- |
| Concurrency mechanism for create-or-increment | `synchronized` block / explicit lock vs. `ConcurrentHashMap.compute` (atomic per-key) vs. per-name `AtomicLong` registry | `ConcurrentHashMap.compute` (or equivalent atomic per-key update) storing an immutable value+timestamp pair | Atomic per key with no explicit locking, directly satisfies the "no lost updates, no distributed lock" constraint; avoids a second map for timestamps that could desync from the value |
| `{name}` validation mechanism | Declarative `@Validated` + `@Pattern` on the `@PathVariable` vs. manual regex check in the service throwing a hand-built exception | Declarative Bean Validation on the path variable | Consistent with how greetings/farewells already validate input; the resulting `ConstraintViolationException` carries the offending property name, avoiding a hand-maintained field-name mapping |
| Where the new `400` handler (`ConstraintViolationException`) lives | New counter-local advice vs. extend the existing shared, app-wide advice | Extend the existing shared advice with one new handler | The `400 validation_failed` shape is generic HTTP-validation infrastructure, not counter domain logic — same category as the existing `MethodArgumentNotValidException`/`HttpMessageNotReadableException` handlers it already carries |
| Where the new `404` handler lives | Add to the shared app-wide advice vs. a new advice scoped to `CounterController` only | New advice scoped to `CounterController` (`assignableTypes`) | "Counter not found" is domain-specific to this feature only; scoping it avoids growing the app-wide advice with a concern no other controller will ever raise |
| Counter-name constants location | Add to `NameValidationConstants` vs. a new, package-local constant | New constant local to the `counter` package | Explicit brief constraint — different rule set (slug vs. person name), different feature; overloading the existing class would conflate two unrelated validation domains |
| Store placement | In the controller vs. behind the service | Behind the service | Explicit brief constraint; also keeps the controller stateless and easy to reason about |
| Value type | `int` vs. `long` | `long` | No max-value cap is in scope; `long` avoids a low, arbitrary overflow ceiling for negligible cost |

## 6. Open questions

- None blocking. Two accepted, explicitly-flagged trade-offs carried into
  implementation:
  1. The shared `GreetingValidationExceptionHandler` will now back **three**
     unrelated features (greeting, farewell, counter) while still living in
     and being named after the `greeting` package. This was already an
     accepted trade-off after farewell; extending it once more for a generic
     `400` concern keeps this feature's diff minimal and consistent with
     precedent, but a future rename/relocation to a neutral package (e.g.
     `web`/`error`) is worth doing before a fourth feature makes the misnomer
     worse. Not addressed in this change — out of this brief's scope.
  2. Unbounded store growth (§3.3) is accepted, matching the service's
     existing no-auth/no-rate-limiting posture.

## 7. Implementation implications

- Add a new `com.example.scratch.counter` package (main + test).
- Add a thin controller with two handlers:
  `POST /api/v1/counters/{name}/increments` (no `@RequestBody`) and
  `GET /api/v1/counters/{name}`; class-level `@Validated`, `{name}` annotated
  with a `@Pattern` referencing the new package-local slug constant.
- Add a service holding a `ConcurrentHashMap`-backed store (name → immutable
  value+timestamp pair); `increment(name)` does one atomic create-or-update;
  `get(name)` returns the current state or signals not-found (e.g. an
  unchecked exception caught by the new counter-scoped advice).
- Add a small response DTO (`name`, `value`, `updatedAt`) reusing `Instant` +
  default Jackson ISO-8601 serialization, same as `FarewellResponse.generatedAt`.
- Add one new `@ExceptionHandler(ConstraintViolationException.class)` to the
  existing `GreetingValidationExceptionHandler`, mapping the violation's
  property-path leaf (not the full `method.name` path) to field `"name"` in
  the standard envelope.
- Add a new, small `@RestControllerAdvice(assignableTypes = CounterController.class)`
  in the `counter` package mapping "not found" to the `404`/`not_found`
  envelope shape.
- Tests per the brief's coverage list: first increment → `value: 1`; second
  increment → `value: 2`; GET after increment matches; GET unknown name →
  `404`; invalid name (uppercase, leading hyphen, too long, double hyphen) →
  `400`; regression checks that `/health`, greetings, and farewells are
  unaffected.
- README gets a brief new section (methods, paths, example), alongside the
  existing greetings/farewells sections.

## 8. Addendum — deep-review hardening

`.deep-review/2026-08-14-feature-named-counter/REPORT.md` raised 4 P2s (no
P0/P1). All four were fixed before push:

- Tightened this document's §3.3 resource-exhaustion rationale (above) to
  name the qualitative difference between this endpoint and the prior
  stateless ones, rather than only citing "mirrors the existing posture."
  No behavior change.
- Added a direct unit test in `GreetingValidationExceptionHandlerTest` for
  the shared advice's `handlePathVariableViolation`/`leafPropertyName`
  (including the no-dot property-path fallback), matching the file's
  existing one-test-per-handler pattern; previously only covered indirectly
  via `CounterControllerTest`.
- Documented the `@Validated` annotation on `CounterController` in-place
  (not just in the class javadoc), since removing it would silently change
  the `400` body shape by switching which exception type Spring raises for
  path-variable violations.
- Added the `RequestSizeLimitFilter`'s own documented `Content-Length`
  caveat to the README's "Request size limit" section, since this change
  broadened that section's claim to explicitly cover the new counter
  endpoints.

No architecture decision from §5 changed; this is hardening within the
already-agreed shape, not a Decision Drift.
