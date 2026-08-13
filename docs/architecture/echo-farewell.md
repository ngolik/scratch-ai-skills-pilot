# Architecture & Design — Echo Farewell API

## 1. Context

- **Problem:** The service exposes `GET /health` and `POST /api/v1/greetings`.
  We need a second, sibling endpoint, `POST /api/v1/farewells`, that validates
  a name/locale pair and returns a localized farewell as structured JSON,
  matching the greeting endpoint's response and error shapes.
- **Goals:** Add the farewell endpoint without disturbing `/health` or
  `/api/v1/greetings`; reuse the existing `400` problem-body contract
  byte-for-byte; support the same three hardcoded locale templates
  (`en`/`es`/`de`).
- **Non-goals:** Auth/API keys, persistence, rate limiting, i18n resource
  bundles beyond the three templates, OpenAPI/Swagger UI, any change to
  `/health` or the greetings contract, CI/deployment changes.
- **Constraints:** Stay on the stack already in the repo (Java 21, Spring Boot
  parent `4.1.0`, Maven, `spring-boot-starter-web` + `spring-boot-starter-validation`,
  both already present); thin controller + small service; reuse existing
  greeting validation/error-shape types where clearly cheaper than
  duplicating; keep farewell as its own endpoint, not merged into greetings.
- **Sources used:** `docs/briefs/echo-farewell.md`; existing code
  (`greeting/` package — `GreetingController`, `GreetingService`,
  `GreetingLocale`, `GreetingRequest`, `GreetingResponse`,
  `GreetingValidationExceptionHandler`, `ValidationErrorResponse`,
  `FieldErrorDetail`); `docs/architecture/echo-greeting.md` (prior decisions
  for this repo).

## 2. Current state (as evidenced)

- The greeting feature already established the pattern this endpoint should
  follow: a thin `@RestController` delegating to a `@Service`, a
  Bean-Validation-annotated request DTO, a plain response DTO, and a locale
  enum holding the fixed message templates.
- The validation-error mapping (`GreetingValidationExceptionHandler`) is a
  **global** `@RestControllerAdvice` — it is not scoped to `GreetingController`
  and reacts to `MethodArgumentNotValidException` /
  `HttpMessageNotReadableException` from *any* controller in the application.
  Its output types, `ValidationErrorResponse` and `FieldErrorDetail`, are
  public records. This means the brief's exact `400` shape is already produced
  for any new `@Valid @RequestBody` controller method added anywhere in the
  app, including a new farewell controller, with **no new error-handling code**
  required.
- `spring-boot-starter-validation` is already a dependency (added for
  greetings), so no new starter is needed for farewell's request validation.

## 3. Recommended architecture

### 3.1 Components and responsibilities

| Component | Responsibility | Notes |
| --- | --- | --- |
| Farewell controller (web layer) | Accepts `POST /api/v1/farewells`, triggers request validation, delegates message building to the service, maps the result to the success JSON shape | New; thin — no template logic here |
| Farewell service | Given a validated name + locale, resolves the correct farewell template and produces the message + generation timestamp | New; holds the only "business logic" (template selection) |
| Locale/template lookup | Maps a locale code to one of the three fixed farewell templates | New, small — mirrors the existing greeting locale lookup; not a shared component with greeting since the message text differs per feature |
| Validation error mapping (web layer) | Converts framework validation failures into the `400` problem-body shape | **Reused as-is** — the existing global advice already covers this controller; no new class |
| `GreetingController` / `GreetingService` / `HealthController` | Unchanged | Existing |

### 3.2 Interactions and data flow

- **Happy path:** Client sends `POST /api/v1/farewells` with `{ name, locale? }`
  → web layer parses JSON and runs request validation → controller calls the
  farewell service with the validated data → service resolves the template for
  the locale (defaulting to `en` when locale is absent) and formats the
  message → controller returns `200` with
  `{ message, name, locale, generatedAt }`.
- **Validation failure path:** Any constraint violation (missing/blank name,
  name too long, disallowed characters, unsupported locale) is caught by the
  existing global validation advice before the service runs → response is
  `400` with `{ error: "validation_failed", details: [{ field, message }, ...] }`
  → identical shape and behavior to the greetings endpoint's error path.
- **Wrong method on this path:** Falls through to Spring's default handling —
  no custom design required, per the brief.
- **Failure/retry/compensation:** Not applicable — synchronous, stateless, no
  external dependencies.

### 3.3 Trust boundaries and data

- **Data classes:** Request/response bodies contain only a user-supplied name
  and locale code and a derived farewell string/timestamp — no sensitive
  data, no persistence. Same posture as greetings.
- **Authn/authz:** None, consistent with the brief's non-goals and the
  existing unauthenticated endpoints.
- **Input handling:** Same constraint as greetings — name is bounded by
  length + character allow-list at the validation boundary before it reaches
  the service or is echoed back in the response.

## 4. Quality attributes

| Attribute | Target / strategy |
| --- | --- |
| Reliability | No external dependencies; only failure mode is caller input, fully handled by the (reused) validation path |
| Observability | None added at this scope, matching greetings/health precedent |
| Performance | Not a concern — synchronous, in-memory template lookup, no I/O |
| Security | Reuses the same input allow-list (characters + length) and never returns stack traces or internal details |

## 5. Decisions

| Decision | Options considered | Choice | Rationale |
| --- | --- | --- | --- |
| Validation-error handling | Duplicate a farewell-scoped `@RestControllerAdvice` + error DTOs vs. reuse the existing global advice and `ValidationErrorResponse`/`FieldErrorDetail` records | Reuse existing global advice and DTOs, unmodified | The existing advice is already unscoped (applies app-wide) and produces the exact contract the brief requires; duplicating it would violate the brief's explicit "reuse ... where clearly cheaper than duplicating" constraint for no behavioral gain |
| Locale/template storage | Share `GreetingLocale` (add farewell templates to it) vs. a new `FarewellLocale` enum | New `FarewellLocale` enum, package-local to `farewell` | The brief requires farewell to stay a separate endpoint, not merged into greetings; overloading `GreetingLocale` with unrelated message text would couple the two features' domain data for no shared behavior beyond the locale code list |
| Package placement | New `com.example.scratch.farewell` package vs. flat/shared with `greeting` | New sibling package `com.example.scratch.farewell`, importing the reused `greeting.ValidationErrorResponse`/`FieldErrorDetail` types | Mirrors the existing `greeting` package structure (`docs/architecture/echo-greeting.md` §Decisions); keeps farewell-specific classes (controller, service, DTOs, locale enum) separate while still reusing the genuinely shared error-shape types |
| Locale defaulting | Require `locale` vs. default to `en` when absent | Default to `en` when absent, reject only when present-and-unsupported | Matches the brief's contract exactly, same as greetings |
| Template storage | Externalized i18n resource bundle vs. hardcoded enum | Hardcoded in code | Explicitly out of scope per the brief |
| Timestamp source | Server clock vs. client-supplied | Server clock (`Instant.now()`), ISO-8601 UTC | Brief specifies `generatedAt` as generated, not client input |
| Layering | Controller-only logic vs. controller + service | Controller + service | Explicit brief constraint, consistent with greetings |

## 6. Open questions

- None blocking. Reusing `greeting.ValidationErrorResponse`/`FieldErrorDetail`
  from the `farewell` package creates a one-way dependency from `farewell` →
  `greeting` for two small, already-public record types; this is called out
  here as an accepted trade-off (per the brief's explicit reuse instruction)
  rather than a gap.

## 7. Implementation implications

- Add a new `com.example.scratch.farewell` package (main + test) mirroring the
  `greeting` package's shape.
- Add a request DTO carrying `name` (required, blank-check, max length,
  character allow-list — same constraints as `GreetingRequest`) and `locale`
  (optional, allow-list of `en`/`es`/`de`).
- Add a response DTO carrying `message`, `name`, `locale`, `generatedAt`.
- Add a `FarewellLocale` enum holding the three farewell templates
  (`en` → `Goodbye, {name}!`, `es` → `¡Adiós, {name}!`,
  `de` → `Auf Wiedersehen, {name}!`).
- Add the farewell service (template resolution + message formatting) and a
  thin controller for `POST /api/v1/farewells`.
- **No new exception-handling class** — confirm in implementation that the
  existing `GreetingValidationExceptionHandler` advice fires correctly for the
  new controller's `@Valid` failures and malformed JSON.
- Add controller/service tests per the brief's coverage list (happy path per
  locale + default, blank name, unsupported locale) plus regression tests
  confirming `/health` and `/api/v1/greetings` are unaffected.
- README gets a brief mention of the new endpoint (method, path, example),
  alongside the existing greetings section.

## 8. Addendum — deep-review hardening

`.deep-review/2026-08-13-feature-echo-farewell/REPORT.md` raised 4 P2s (no
P0/P1). All four were fixed before push:

- Added an `IllegalArgumentException` handler to the shared, app-wide
  `GreetingValidationExceptionHandler` so a future drift between either
  locale enum and its request DTO's `@Pattern` degrades to the standard
  `400` shape instead of an unhandled `500` — covers both `greeting` and
  `farewell`.
- Documented the `farewell` → `greeting` exception-handling dependency with
  in-code comments on both `GreetingValidationExceptionHandler` and
  `FarewellController`, since it was otherwise invisible (no import, only
  Spring's component scan).
- Extracted `MAX_NAME_LENGTH` / `NAME_PATTERN` / `LOCALE_PATTERN` into a new
  shared `com.example.scratch.validation.NameValidationConstants`, referenced
  by both `GreetingRequest` and `FarewellRequest`, so the README's "identical
  validation" claim is enforced rather than merely copy-pasted.
- Added `RequestSizeLimitFilter` (app-wide, `com.example.scratch` root
  package) rejecting request bodies over 4096 bytes with `413` before
  Jackson/Bean Validation run — closes the pre-existing unbounded-body gap
  for both `/api/v1/greetings` and `/api/v1/farewells` in one change.

No architecture decision from §5 changed; this is hardening within the
already-agreed shape, not a Decision Drift.
