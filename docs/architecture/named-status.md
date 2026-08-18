# Architecture & Design — named-status

## 1. Context

- **Problem:** Clients need to set and read a single named "status" message
  (arbitrary short text, e.g. "away"), stored only in process memory, via a
  small JSON HTTP API — analogous in shape to `named-counter` and
  `named-toggle` but with a string payload instead of an increment/boolean,
  and the same classic Spring layering requirement as `named-toggle`.
- **Goals:** `PUT`/`GET /api/v1/statuses/{name}` with create-or-replace
  semantics, reusing this service's existing `400 validation_failed` and
  `404 not_found` error shapes; code split into
  controller/dto/service/entity/repository/config with enforced dependency
  direction (controller → service → repository).
- **Non-goals:** persistence beyond process memory, auth, list/delete/TTL,
  default-on-missing GET, hexagonal packaging, refactoring `greeting` /
  `farewell` / `counter` / `notes` / `toggle` into this layout.
- **Constraints:** stay on Java 21 + Spring Boot 4.1.0; do not overload
  `validation.NameValidationConstants` (person names) or merge into
  `toggle`/`counter` packages; reuse `greeting.ValidationErrorResponse` /
  `FieldErrorDetail` — do not invent a third error document type; 4096-byte
  body cap already applies globally.
- **Sources used:** `docs/briefs/named-status.md`, `docs/architecture/named-toggle.md`
  (closest precedent — same layering requirement), code areas
  `toggle/*` (layered-package precedent), `notes/domain/NoteText.java`
  (existing trim/blank/max-length invariant precedent),
  `greeting/GreetingValidationExceptionHandler.java`.

## 2. Current state (as evidenced)

- `toggle/` is the direct analog and the only other feature already using the
  controller/dto/service/entity/repository/config split — same PUT/GET shape,
  same feature-local name-pattern constant pattern (`ToggleNameConstants`,
  deliberately separate from `validation.NameValidationConstants`), same
  package-scoped `@RestControllerAdvice(assignableTypes = ...)` for its
  not-found `404`, same explicit `@Bean` wiring in a `config` class, same
  `ConcurrentHashMap`-backed in-memory repository.
- App-wide `greeting.GreetingValidationExceptionHandler` (unscoped
  `@RestControllerAdvice`) already handles, for every controller in the app:
  `MethodArgumentNotValidException` (`@Valid` body failures on missing/null
  fields), `HttpMessageNotReadableException` (malformed JSON),
  `ConstraintViolationException` (path-variable `@Pattern` failures) — all
  mapped to `400 validation_failed`. Status name and "message present"
  validation reuse this handler as-is; no new advice needed for those cases.
- `notes/domain/NoteText.java` already implements exactly the
  trim-then-validate-length invariant this brief asks for (trim, reject blank,
  reject over a max length), currently inside a domain record's compact
  constructor. Bean Validation's `@Size` cannot express this correctly on its
  own, because `@Size` measures the **raw** (untrimmed) string — a value with
  leading/trailing spaces that is 82 raw characters but 79 trimmed characters
  must pass, which a raw-length `@Size(max=80)` would wrongly reject.
- No existing feature validates a free-text field with a trim step before
  length-checking through the shared DTO-level `@Valid` pipeline; this is new
  for `named-status`.

## 3. Recommended architecture

### 3.1 Components and responsibilities

| Component | Responsibility | Notes |
| --- | --- | --- |
| `status.controller` | HTTP routes (`PUT`/`GET /api/v1/statuses/{name}`), status codes, path-variable name validation | Accepts/returns DTOs only; depends on `status.service` only |
| `status.dto` | `SetStatusRequest` (request), `StatusResponse` (response) | No dependency on entity or repository |
| `status.service` | Validate `message` (trim, blank, max-length) at the use-case level, upsert/get, map entity ↔ dto, raise not-found | Depends on `status.entity` + `status.repository`; only component that touches both; owns the trim/length invariant per brief's layer table |
| `status.entity` | `Status` stored model (`name`, `message`, `updatedAt`) | Never serialized directly as an HTTP response |
| `status.repository` | In-memory save/find-by-name for `Status` | No HTTP types, no DTO types, no framework web annotations |
| `status.config` | Wires the in-memory repository bean | Only place that instantiates the repository implementation |
| `status.controller` (exception mapping) | Maps "status not found" (`404`) and "message invalid after trim" (`400`) to the shared envelopes | Package-scoped advice, same pattern as `toggle.controller.ToggleExceptionHandler` |

### 3.2 Interactions and data flow

- **PUT (create-or-replace), happy path:** controller validates `{name}`
  (path pattern) and binds the JSON body (`message` required non-null string)
  → service trims `message`, validates blank/max-length, upserts via
  repository → service maps the returned entity to `StatusResponse` (trimmed
  `message`) → controller returns `200`.
- **GET, happy path:** controller validates `{name}` → service asks
  repository for the entity → maps to `StatusResponse` → controller returns
  `200`.
- **GET, unknown name:** repository returns empty → service raises a
  status-not-found signal → package-scoped advice returns `404 not_found`
  (same envelope shape as counters/toggles).
- **Invalid `{name}`:** caught by the existing app-wide
  `greeting.GreetingValidationExceptionHandler` (`ConstraintViolationException`
  path) → `400 validation_failed`, field `"name"`.
- **Missing `message` (null / absent field):** caught by the same app-wide
  handler's `MethodArgumentNotValidException` path (`@NotNull` on the DTO) →
  `400 validation_failed`, field `"message"`.
- **Blank (whitespace-only) or too-long-after-trim `message`:** DTO-level
  `@NotNull` passes (value is present), so this is a business-rule check, not
  a shape check — `status.service` trims and validates length, raising a
  domain exception → mapped by the package-scoped `status.controller` advice
  to the **same** `400 validation_failed` shape, field `"message"`.
- **Failure / retry / compensation:** none — single-process in-memory map, no
  external calls, no retries.

### 3.3 Trust boundaries and data

- Data class: process-local, non-sensitive (status name + free-text message +
  timestamp). No PII, no secrets.
- Authn/authz: none, unchanged for this service.
- Request bodies are untrusted input; validated via Bean Validation (name
  shape, message presence) plus a service-level trim/length invariant
  (message content), and the existing global body-size filter — no new size
  check per brief.

## 4. Quality attributes

| Attribute | Target / strategy |
| --- | --- |
| Reliability | Single `ConcurrentHashMap`-backed repository; atomic upsert per key (same pattern as `CounterService`/`ToggleService`), so concurrent `PUT`s on the same name never interleave-corrupt state |
| Observability | None added beyond existing app defaults — out of scope per brief |
| Performance | O(1) in-memory lookups; no I/O; not a concern at this scale |
| Security | No new attack surface beyond existing validated-input pattern; no stack traces in error bodies (unchanged app behavior) |

## 5. Decisions

| Decision | Options considered | Choice | Rationale |
| --- | --- | --- | --- |
| Package layout | Flat (like `counter/`) vs. layered (`controller`/`dto`/`service`/`entity`/`repository`/`config`) | Layered | Brief explicitly requires classic Spring layers for this feature; mirrors `toggle/`, the only other feature under this requirement |
| `message` trim/length validation ownership | (a) `@Size` on the DTO, (b) trim-then-validate inside the entity's constructor (`NoteText` style), (c) trim-then-validate in `status.service` | (c) service | Brief's layer table assigns "validate name/message" to **service**, not entity or DTO; `@Size` alone is wrong here because it measures the raw, untrimmed string (see §2); keeps the entity a plain stored record with no invented invariant logic, unlike `NoteText` |
| `message` "missing" vs. "blank/too-long" split | Single service-level check for all message failures vs. splitting missing (DTO `@NotNull`) from blank/too-long (service) | Split | Matches the existing app-wide `400` pipeline (`MethodArgumentNotValidException` for null/missing already flows through `GreetingValidationExceptionHandler` with zero new code); only the trim-dependent business rule needs new logic |
| Repository wiring | `@Repository`-annotated impl auto-detected vs. explicit `@Bean` in a `config` class | Explicit `@Bean` in `status.config` | Mirrors `toggle.config`; brief calls out `config` as the layer that "wires the in-memory repository" |
| Status name validation | Reuse `validation.NameValidationConstants` vs. new feature-local constant | New feature-local constant in `status.controller` | Brief forbids overloading the person-name constants; mirrors `CounterNameConstants` / `ToggleNameConstants`, one per feature |
| `400`/`404` error handling | New status-specific advice for all cases vs. reuse existing components where possible | Reuse app-wide `GreetingValidationExceptionHandler` for name-shape and message-missing `400`s; add one small package-scoped advice (mirroring `ToggleExceptionHandler`) for message-invalid-after-trim `400` and not-found `404` | Brief requires reusing existing shapes and forbids a third error document type |
| Not-found / message-invalid exception ownership | Define in `status.controller` (co-located with handler) vs. in `status.service` (co-located with the code that raises it) | `status.service` | Same reasoning `named-toggle`'s review already established: defining exceptions in `controller` would force `status.service` to import a `status.controller` type, reversing the brief's required `controller → service → repository` direction. Defining them in `service` lets `status.controller`'s advice import downward instead |
| Entity vs. DTO for `PUT`/`GET` responses | Return entity directly vs. map to a response DTO | Map to `StatusResponse` in `status.service` | Brief: "entity must not be the HTTP response body" |

## 6. Open questions

None — brief marks open questions as resolved (in-memory only, GET unknown is
`404`, no list/delete).

## 7. Implementation implications

- New package tree under `src/main/java/com/example/scratch/status/` with the
  six sub-packages above; no changes to `greeting/`, `farewell/`, `counter/`,
  `notes/`, `toggle/`, or `validation/`.
- Impl plan must enumerate: DTOs (`SetStatusRequest`, `StatusResponse`),
  entity (`Status`), repository interface + in-memory implementation,
  service (including the trim/blank/max-length invariant and the not-found
  exception), controller, config bean, package-scoped advice for `404` +
  message-invalid `400`, and a feature-local name-pattern constant.
- Impl plan must cover the layering/dependency-direction check called out in
  the brief's test requirements (controller does not import repository;
  repository does not import dto) as part of the test suite, alongside the
  WebMvc happy-path/validation/404/regression tests.
- README update: brief lines for `PUT`/`GET /api/v1/statuses/{name}` with an
  example, alongside the existing counter/toggle entries.
- No changes required to `RequestSizeLimitFilter`, `HealthController`, or any
  other existing controller/config.

Suggested branch: `feature/named-status` (already created).
