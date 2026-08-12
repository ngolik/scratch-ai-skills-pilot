# Implementation Plan — Echo Greeting API

## Summary

- **Goal:** Add `POST /api/v1/greetings`, returning a localized JSON greeting
  for a validated `name`/`locale` pair, with a `400` problem body for invalid
  input, without changing `GET /health`.
- **In scope:** Request/response DTOs, a greeting service (template
  resolution), a thin controller, global validation-error mapping, unit/WebMvc
  tests, README update.
- **Out of scope:** Auth, persistence, rate limiting, i18n bundles, OpenAPI UI,
  any change to `/health` or CI (per architecture doc §1).
- **Architecture input:** `docs/architecture/echo-greeting.md`

## Touch map

| Area | Why |
| --- | --- |
| `pom.xml` | Add `spring-boot-starter-validation` (Bean Validation) — new dependency per architecture §5 |
| `src/main/java/com/example/scratch/greeting/` (new package) | Holds the new endpoint's controller, service, DTOs, and error mapping — kept separate from the root package's `HealthController` since this introduces several new classes |
| `src/test/java/com/example/scratch/greeting/` (new package) | WebMvc/unit tests for the new endpoint |
| `README.md` | Add a brief mention of the new endpoint (method, path, example) |

`[INFERRED — please validate]` New classes live under `com.example.scratch.greeting`
rather than flat in `com.example.scratch` (where `HealthController` sits),
since this feature has enough classes (controller, service, 2 DTOs, error
handler) to warrant its own package. `HealthController` itself is untouched.

## Steps

### Step 1 — Add validation dependency and request/response DTOs

- **Outcome:** `pom.xml` pulls in Bean Validation; two plain DTOs exist
  (`GreetingRequest` with constrained `name`/`locale`, `GreetingResponse` with
  `message`/`name`/`locale`/`generatedAt`), with no wiring yet.
- **Approach:**
  - Add `spring-boot-starter-validation` to `pom.xml`.
  - `GreetingRequest`: `name` — `@NotBlank`, `@Size(max = 40)`, a pattern
    constraint restricting to letters/spaces/hyphen/apostrophe; `locale` —
    optional field, validated against the `en`/`es`/`de` allow-list only when
    present (defaulting happens in the service, not via a bean-validation
    default, since Bean Validation doesn't set defaults on missing fields).
  - `GreetingResponse`: plain record/POJO with the four response fields;
    `generatedAt` serializes as an ISO-8601 UTC string (`Instant` serializes
    this way by default with Jackson's JSR-310 support already on the
    classpath via `spring-boot-starter-web`).
- **Tests:** None yet at this step (no behavior to exercise) — covered once
  wired to the controller in Step 2/3.
- **Done when:** Project compiles with the new dependency and DTOs; no
  endpoint yet.

### Step 2 — Greeting service and happy-path controller wiring

- **Outcome:** `POST /api/v1/greetings` returns `200` with the correct message
  for `en` (default and explicit), `es`, and `de`.
- **Approach:**
  - `GreetingService`: given a validated name and an optional locale, resolves
    the locale (default `en` when absent) against the three fixed templates
    (`en` → `Hello, {name}!`, `es` → `¡Hola, {name}!`, `de` → `Hallo, {name}!`)
    and returns a `GreetingResponse` with `Instant.now()` as `generatedAt`.
  - `GreetingController`: `@RestController` with `@PostMapping("/api/v1/greetings")`,
    `@Valid @RequestBody GreetingRequest`, delegates to `GreetingService`,
    returns the `GreetingResponse` directly (200 default).
- **Tests:** `@WebMvcTest(GreetingController.class)` (mocking the service) or a
  slice test exercising the real service — assert `200` and the exact message
  for each of `en` (explicit), `es`, `de`, and default-locale (`locale`
  omitted → treated as `en`).
- **Done when:** All four happy-path cases pass and match the architecture
  doc's response shape exactly.

### Step 3 — Validation error mapping (400 problem body)

- **Outcome:** Missing/blank `name`, too-long `name`, disallowed characters,
  and unsupported `locale` each return `400` with
  `{ "error": "validation_failed", "details": [{ "field", "message" }, ...] }`
  and no stack trace.
- **Approach:** Add a `@RestControllerAdvice` handling
  `MethodArgumentNotValidException` (thrown by `@Valid` failures), mapping
  each field error to a `{field, message}` entry and wrapping them in the
  brief's shape with HTTP `400`. Confirm this advice is scoped to not affect
  `/health` (it only triggers on `@Valid`-annotated request bodies, which
  `/health` has none of).
- **Tests:** WebMvc tests for: blank `name`, `name` over 40 chars, `name` with
  a disallowed character, unsupported `locale` value (e.g. `"fr"`) — each
  asserting `400`, `error: "validation_failed"`, and that `details` contains
  an entry for the offending field. One test also asserts a valid request
  with an *absent* `locale` is **not** rejected (confirms default-vs-invalid
  distinction from architecture §5).
- **Done when:** All listed validation cases return the exact `400` shape;
  existing `/health` test still passes unmodified.

### Step 4 — README update

- **Outcome:** README documents the new endpoint briefly.
- **Approach:** Add a short section (method, path, one example request/response
  pair) near the existing "Health check" section — no full contract
  duplication, just enough for a reader to try it via `curl`.
- **Tests:** Manual review only — confirm the example matches actual behavior
  from Steps 2–3.
- **Done when:** A reader unfamiliar with the repo can `curl` the new endpoint
  using only the README.

## Risks and mitigations

| Risk | Mitigation |
| --- | --- |
| `@Valid` silently no-ops if `spring-boot-starter-validation` isn't actually on the classpath | Step 1 done-when explicitly requires a clean compile with the new dependency present; Step 2/3 tests will fail loudly (no 400s) if validation isn't wired |
| Global `@RestControllerAdvice` accidentally intercepts unrelated exceptions (e.g. malformed JSON body) and returns a shape not covered by the brief | Scope the advice narrowly to `MethodArgumentNotValidException` only in this change; malformed-JSON handling is not in the brief's acceptance criteria, so leave it to framework defaults rather than over-building |
| Jackson serializes `Instant` in an unexpected shape if JSR-310 module isn't auto-registered | Step 2 test asserts the exact `generatedAt` format; `spring-boot-starter-web` auto-configures this module by default, so this is a low risk, verified by test rather than assumed |

## Open questions

- Package placement (`com.example.scratch.greeting` vs. flat) — see Touch map
  gap marker; low-stakes, does not block starting Step 1.
