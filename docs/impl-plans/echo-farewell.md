# Implementation Plan — Echo Farewell API

## Summary

- **Goal:** Add `POST /api/v1/farewells`, returning a localized JSON farewell
  for a validated `name`/`locale` pair, reusing the existing `400` problem
  body contract, without changing `GET /health` or `POST /api/v1/greetings`.
- **In scope:** Request/response DTOs, a `FarewellLocale` enum, a farewell
  service, a thin controller, unit/WebMvc tests, README update.
- **Out of scope:** Auth, persistence, rate limiting, i18n bundles, OpenAPI
  UI, any change to `/health` or greetings, CI (per architecture doc §1). No
  new exception-handling class — the existing global advice is reused as-is.
- **Architecture input:** `docs/architecture/echo-farewell.md`

## Touch map

| Area | Why |
| --- | --- |
| `src/main/java/com/example/scratch/farewell/` (new package) | Holds the new endpoint's controller, service, DTOs, locale enum — mirrors the `greeting` package |
| `src/test/java/com/example/scratch/farewell/` (new package) | WebMvc/unit tests for the new endpoint |
| `src/test/java/com/example/scratch/greeting/` and `HealthControllerTest` | No code changes; re-run as regression evidence that greetings/health are unaffected |
| `README.md` | Add a brief mention of the new endpoint (method, path, example) |

No changes to `pom.xml` (`spring-boot-starter-validation` already present) or
to the `greeting` package — `FarewellController`/`FarewellRequest` import
`greeting.ValidationErrorResponse`/`FieldErrorDetail` types only for test
assertions and rely on the existing global `GreetingValidationExceptionHandler`
advice at runtime (architecture §5).

## Steps

### Step 1 — Request/response DTOs and locale enum

- **Outcome:** `FarewellRequest` (constrained `name`/`locale`),
  `FarewellResponse` (`message`/`name`/`locale`/`generatedAt`), and
  `FarewellLocale` (three templates) exist, with no wiring yet.
- **Approach:**
  - `FarewellRequest`: same constraints as `GreetingRequest` — `name` via
    `@NotBlank`, `@Size(max = 40)`, letters/spaces/hyphen/apostrophe pattern;
    `locale` — optional, validated against `en`/`es`/`de` only when present.
  - `FarewellResponse`: plain record with the four response fields;
    `generatedAt` as `Instant` (ISO-8601 UTC via existing Jackson JSR-310
    support).
  - `FarewellLocale` enum mirroring `GreetingLocale`'s `resolve`/`formatMessage`
    shape, with templates `en` → `Goodbye, {name}!`, `es` → `¡Adiós, {name}!`,
    `de` → `Auf Wiedersehen, {name}!`.
- **Tests:** None yet at this step — covered once wired in Step 2/3.
- **Done when:** Project compiles with the new package; no endpoint yet.

### Step 2 — Farewell service and happy-path controller wiring

- **Outcome:** `POST /api/v1/farewells` returns `200` with the correct message
  for `en` (default and explicit), `es`, and `de`.
- **Approach:**
  - `FarewellService`: given a validated name and optional locale, resolves
    the locale (default `en` when absent) against `FarewellLocale` and
    returns a `FarewellResponse` with `Instant.now()` as `generatedAt`.
  - `FarewellController`: `@RestController` with
    `@PostMapping("/api/v1/farewells")`, `@Valid @RequestBody FarewellRequest`,
    delegates to `FarewellService`, returns `FarewellResponse` directly
    (`200` default).
- **Tests:** `@WebMvcTest(FarewellController.class)` — assert `200` and the
  exact message for `en` (explicit), `es`, `de`, and default-locale (`locale`
  omitted → treated as `en`).
- **Done when:** All four happy-path cases pass and match the architecture
  doc's response shape exactly.

### Step 3 — Confirm reused validation-error path

- **Outcome:** Missing/blank `name`, too-long `name`, disallowed characters,
  and unsupported `locale` on `/api/v1/farewells` each return `400` with the
  same `{ "error": "validation_failed", "details": [...] }` shape as
  greetings — produced by the *existing* `GreetingValidationExceptionHandler`
  advice, with no new handler code.
- **Approach:** No production code change in this step (advice is already
  global). Add WebMvc tests against `/api/v1/farewells` to prove the reuse
  actually works end-to-end for this new controller, since this is the
  architecture's key risk area (§6 open question / reuse decision).
- **Tests:** WebMvc tests for: blank `name`, `name` over 40 chars, `name`
  with a disallowed character, unsupported `locale` (e.g. `"fr"`) — each
  asserting `400`, `error: "validation_failed"`, and a `details` entry for
  the offending field.
- **Done when:** All listed validation cases return the exact `400` shape on
  `/api/v1/farewells`; existing `GreetingControllerTest` and
  `HealthControllerTest` still pass unmodified (regression evidence for G3).

### Step 4 — README update

- **Outcome:** README documents the new endpoint briefly.
- **Approach:** Add a short section (method, path, one example request/response
  pair) near the existing "Greeting API" section — no full contract
  duplication.
- **Tests:** Manual review only — confirm the example matches actual behavior
  from Steps 2–3.
- **Done when:** A reader unfamiliar with the repo can `curl` the new endpoint
  using only the README.

## Risks and mitigations

| Risk | Mitigation |
| --- | --- |
| Existing `GreetingValidationExceptionHandler` turns out to be scoped in a way that doesn't actually catch `FarewellController` exceptions (e.g. hidden `basePackages`/`assignableTypes` restriction) | Re-verified by reading the class in architecture (no such restriction present); Step 3 tests will fail loudly (no `400`s, or a default Spring error shape) if the assumption is wrong, at which point a farewell-scoped advice would need to be added and the architecture doc updated (Decision Drift) |
| Duplicated locale-resolution logic between `GreetingLocale` and `FarewellLocale` (two near-identical enums) | Accepted per architecture §5 — keeps the two features' message domains decoupled; revisit only if a third locale-driven endpoint appears and a shared abstraction becomes clearly justified |
| Jackson serializes `Instant` unexpectedly | Step 2 test asserts the exact `generatedAt` format; same mechanism already verified working for greetings |

## Open questions

- None blocking.
