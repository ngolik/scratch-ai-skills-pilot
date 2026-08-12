# Architecture & Design — Echo Greeting API

## 1. Context

- **Problem:** The pilot service currently exposes only `GET /health`. We need a
  second endpoint, `POST /api/v1/greetings`, that validates a name/locale pair
  and returns a localized greeting as structured JSON.
- **Goals:** Add a small, well-validated JSON API on the existing service
  without disturbing `/health`; return a `400` problem body (no stack traces)
  for invalid input; support three hardcoded locale templates.
- **Non-goals:** Auth/API keys, persistence, rate limiting, i18n resource
  bundles beyond the three templates, OpenAPI/Swagger UI, changes to `/health`,
  CI/deployment changes.
- **Constraints:** Stay on the stack already in the repo (Java 21, Spring Boot
  parent `4.1.0`, Maven, `spring-boot-starter-web`); prefer a thin controller +
  small service over controller-only logic.
- **Sources used:** `docs/briefs/echo-greeting.md`; existing code
  (`HealthController.java`, `pom.xml`); `docs/architecture/health-endpoint.md`
  (prior decisions for this repo).

## 2. Current state (as evidenced)

- The service has one component: a single `@RestController` (`HealthController`)
  answering `GET /health` with plain text `ok`. No service layer, no JSON
  (de)serialization, and no bean-validation dependency are wired up yet.
- `spring-boot-starter-web` is already a dependency, so JSON support (Jackson)
  and Spring MVC's exception-handling hooks (`@ExceptionHandler` /
  `ResponseEntityExceptionHandler`) are available with no new starter.
  Request-body **validation** (`jakarta.validation` / Bean Validation) is
  *not* currently pulled in transitively by `spring-boot-starter-web` on this
  Boot line, so it is a new, small addition — see §5.
- `[INFERRED — please validate]` The prior architecture doc for `/health`
  states "Spring Boot 3.x," but `pom.xml` pins the `spring-boot-starter-parent`
  to `4.1.0`. This is pre-existing drift from before this change; it is called
  out here only so the new work is documented against the actual current
  version, not the stale claim in the older doc.

## 3. Recommended architecture

### 3.1 Components and responsibilities

| Component | Responsibility | Notes |
| --- | --- | --- |
| Greeting controller (web layer) | Accepts `POST /api/v1/greetings`, triggers request validation, delegates message building to the service, maps the result to the success JSON shape | New; thin — no template logic here |
| Greeting service | Given a validated name + locale, resolves the correct template and produces the message + generation timestamp | New; holds the only "business logic" (template selection), kept out of the controller per the brief's constraint |
| Locale/template lookup | Maps a locale code to one of the three fixed message templates | Could be a small enum or map owned by the service — an implementation detail, not a separate architectural component |
| Validation error mapping (web layer) | Converts framework validation failures into the `400` problem-body shape (`error` + `details[]`) | New; a global exception-handling concern, not per-controller logic, so other endpoints added later inherit the same error shape |
| `HealthController` | Unchanged — `GET /health` behavior is untouched by this change | Existing |

### 3.2 Interactions and data flow

- **Happy path:** Client sends `POST /api/v1/greetings` with `{ name, locale? }`
  → web layer parses JSON and runs request validation → controller calls the
  service with the validated data → service resolves the template for the
  locale (defaulting to `en` when locale is absent) and formats the message →
  controller returns `200` with `{ message, name, locale, generatedAt }`.
- **Validation failure path:** Any constraint violation (missing/blank name,
  name too long, disallowed characters, unsupported locale) is caught by the
  validation error mapping component before the service runs → response is
  `400` with `{ error: "validation_failed", details: [{ field, message }, ...] }`
  → no stack trace or internal exception detail is exposed.
- **Wrong method on this path:** Falls through to Spring's default handling
  (e.g. `405`) — no custom design required, per the brief.
- **Failure/retry/compensation:** Not applicable — the endpoint is synchronous,
  stateless, and has no external dependencies to fail against.

### 3.3 Trust boundaries and data

- **Data classes:** Request/response bodies contain only a user-supplied name
  and locale code and a derived greeting string/timestamp — no sensitive data,
  no persistence.
- **Authn/authz:** None, consistent with the brief's non-goals and with the
  existing unauthenticated `/health` endpoint.
- **Input handling:** This is the service's first endpoint that accepts
  untrusted input. The name field must be constrained (length + character
  allow-list) at the validation boundary before it reaches the service or the
  response — this prevents unbounded or malformed strings from being echoed
  back in the JSON body.

## 4. Quality attributes

| Attribute | Target / strategy |
| --- | --- |
| Reliability | No external dependencies; the only failure mode is caller input, which is fully handled by the validation path (no unhandled exceptions should reach the client) |
| Observability | None added at this scope, matching the existing `/health` precedent — the JSON response and HTTP status are the signal |
| Performance | Not a concern at this scope — synchronous, in-memory template lookup, no I/O |
| Security | Input allow-list (characters + length) prevents obviously malformed input from being reflected back in the response; no stack traces or internal details are ever returned to the client |

## 5. Decisions

| Decision | Options considered | Choice | Rationale |
| --- | --- | --- | --- |
| Validation mechanism | Bean Validation (`jakarta.validation` annotations + `spring-boot-starter-validation`) vs. hand-written manual checks in the service | Bean Validation via `spring-boot-starter-validation` | Declarative constraints on the request DTO keep the controller/service thin (per brief constraint) and give a uniform way to collect *all* violations for the `details[]` array in one pass, rather than hand-rolled if/else chains |
| Error response shape | Spring's default `ProblemDetail`/RFC 7807 vs. the brief's custom `{error, details[]}` shape | Brief's custom shape, via a `@RestControllerAdvice` | The brief specifies an exact JSON contract; a global advice keeps this mapping in one place so future endpoints can reuse it |
| Locale defaulting | Require `locale` vs. default to `en` when absent | Default to `en` when absent, reject only when present-and-unsupported | Matches the brief's request/response contract exactly (`locale` is optional with a stated default) |
| Template storage | Externalized i18n resource bundle vs. hardcoded map/enum in code | Hardcoded in code | Explicitly out of scope per the brief ("i18n resource bundles beyond the three hardcoded templates") |
| Timestamp source | Server clock (`Instant.now()`) vs. client-supplied | Server clock, ISO-8601 UTC | Brief specifies `generatedAt` as a generated value, not client input; UTC avoids timezone ambiguity |
| Layering | Controller-only logic vs. controller + service | Controller + service | Explicit brief constraint: "prefer a thin controller + small service ... over putting all logic in the controller" |

## 6. Open questions

- None blocking. The Bean Validation starter is a new dependency addition;
  this is called out as a decision (§5) rather than a gap, since the brief
  already implies validation is required and no equivalent exists in the repo
  today.

## 7. Implementation implications

- Add the Bean Validation starter dependency (new; `/health` did not need it).
- Add a request DTO carrying `name` (required, blank-check, max length,
  character allow-list) and `locale` (optional, allow-list of `en`/`es`/`de`).
- Add a response DTO carrying `message`, `name`, `locale`, `generatedAt`.
- Add the greeting service (template resolution + message formatting) as a
  separate class from the controller.
- Add a global exception-handling component that maps validation failures to
  the brief's `400` JSON shape; verify it does not also affect `/health`
  (it shouldn't, since `/health` has no request body to validate).
- Add controller/service tests per the brief's coverage list (happy path,
  each locale, blank name, unsupported locale) — implementation plan to detail
  exact test classes.
- README gets a brief mention of the new endpoint (method, path, example).
