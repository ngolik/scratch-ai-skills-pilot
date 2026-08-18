# Architecture & Design — named-label

## 1. Scope and constraints

- **Problem:** Clients need to set and read a single named "label" (short
  free-text value, e.g. `"beta"`) in process memory via a small JSON HTTP
  API — same shape as `named-counter` (flat package, no classic layers) but
  with a string body instead of an increment, and with a trim/length
  invariant on that string (same category of problem `named-status` already
  solved for its `message` field).
- **Goals:** `PUT`/`GET /api/v1/labels/{name}` with create-or-replace
  semantics; reuse this service's existing `400 validation_failed` /
  `404 not_found` envelopes; stay in **one flat package**, controller +
  service only — no dto/entity/repository/config split.
- **Non-goals:** persistence, auth, list/delete/TTL, classic Spring layers,
  hexagonal packaging, OpenAPI, touching `greeting`/`farewell`/`counter`/
  `notes`/`toggle`/`status`.
- **Constraints:** stay on Java 21 + Spring Boot 4.1.0; reuse
  `greeting.ValidationErrorResponse`/`FieldErrorDetail` — no new error
  document type; reuse the existing global 413 body-size filter, no new
  size check; do not reuse `validation.NameValidationConstants` (person
  names) — one feature-local name-pattern constant per feature is the
  established convention.
- **Sources used:** `docs/briefs/named-label.md`; code areas `counter/*`
  (flat-package, no-body precedent — controller/service/name-constants/
  not-found-exception/handler split), `status/service/StatusService.java`
  (trim-then-validate-length invariant precedent), `greeting/*`
  (`GreetingRequest`, `GreetingValidationExceptionHandler`,
  `ValidationErrorResponse`, `FieldErrorDetail` — shared `400` pipeline for
  missing/malformed body fields and path-variable pattern violations).

## 2. Existing shape to respect

- `counter/` is the direct structural analog: one flat package with
  `CounterController`, `CounterService` (`ConcurrentHashMap`-backed,
  atomic `compute` upsert), a package-private `CounterNameConstants`, a
  `CounterNotFoundException`, and a package-scoped
  `@RestControllerAdvice(assignableTypes = CounterController.class)`
  handling only the "not found" `404`. `@Validated` on the controller class
  routes `@PathVariable @Pattern` failures through
  `ConstraintViolationException`, which the **app-wide**
  `greeting.GreetingValidationExceptionHandler` already turns into
  `400 validation_failed` — no new code needed for invalid `{name}`.
- That same app-wide handler already turns a missing/null `@RequestBody`
  field (`MethodArgumentNotValidException`, via a DTO's `@NotNull`) into
  `400 validation_failed` with the DTO's field name — used by
  `greeting.GreetingRequest` today. No new code needed for a missing
  `value`.
- `status/service/StatusService.java` already implements the exact
  trim-then-validate invariant this brief needs for `value` (trim, reject
  blank, reject over a max length) as a **service-level** check, not a
  Bean Validation annotation — because `@Size` measures the raw
  (untrimmed) string, so a value that is 34 raw characters but 32 trimmed
  characters must pass, which `@Size(max=32)` alone would wrongly reject.
  `named-status` is layered (`service` is its own package), but the
  invariant logic itself — not the package split — is the reusable part
  here.
- No existing **flat**-package feature combines a request body with a
  trim/length invariant; `named-label` is the first.

## 3. Recommended change

| Component | Responsibility | Notes |
| --- | --- | --- |
| `label.LabelController` | `PUT`/`GET /api/v1/labels/{name}` routes, path-variable `{name}` pattern validation, `@Valid @RequestBody` binding | Mirrors `CounterController`; `@Validated` on the class for the same `ConstraintViolationException` routing reason |
| `label.LabelRequest` | Request DTO, one field `value` with `@NotNull` only | Presence-only check; blank/length after trim is a business rule, not a shape rule — mirrors why `status` doesn't use `@Size` |
| `label.LabelResponse` | Response DTO: `name`, `value` (trimmed), `updatedAt` | Same triple as `CounterResponse`'s shape, string instead of long |
| `label.LabelService` | Trim `value`, reject blank/over-32-after-trim, upsert (create-or-replace) atomically, get-or-throw-not-found | `ConcurrentHashMap` + `compute`, same atomicity pattern as `CounterService` |
| `label.LabelNameConstants` | Feature-local `{name}` pattern, identical rule to `CounterNameConstants` | Package-private constant class; not shared with `counter`/`toggle`/`status`, per established one-per-feature convention |
| `label.LabelNotFoundException` / `label.LabelValueInvalidException` | Signal "unknown name" / "blank or too-long after trim" | Plain unchecked exceptions, same style as `CounterNotFoundException` / `StatusMessageInvalidException` |
| `label.LabelExceptionHandler` | Package-scoped advice: not-found → `404 not_found` field `"name"`; value-invalid → `400 validation_failed` field `"value"` | `@RestControllerAdvice(assignableTypes = LabelController.class)`, same scoping as `CounterExceptionHandler` |

- **Happy path (PUT):** controller validates `{name}` + binds body →
  service trims `value`, validates blank/length, upserts via `compute` →
  returns `LabelResponse` → controller returns `200`.
- **Happy path (GET):** controller validates `{name}` → service looks up
  by name, throws not-found if absent → controller returns `200`.
- **Invalid `{name}`:** app-wide handler, `ConstraintViolationException` →
  `400 validation_failed`, field `"name"` (no new code).
- **Missing `value` (null/absent):** app-wide handler,
  `MethodArgumentNotValidException` → `400 validation_failed`, field
  `"value"` (no new code).
- **Blank or too-long-after-trim `value`:** `LabelService` raises
  `LabelValueInvalidException` → `LabelExceptionHandler` → `400
  validation_failed`, field `"value"`.
- **Unknown name on GET:** `LabelService` raises `LabelNotFoundException` →
  `LabelExceptionHandler` → `404 not_found`, field `"name"`.
- **Failure / retry / compensation:** none — single-process in-memory map,
  no external calls.
- **Data classes:** process-local, non-sensitive short text. No PII, no
  secrets. No authn/authz, unchanged for this service.

## 4. Key decisions and risks

| Decision | Options considered | Choice | Rationale |
| --- | --- | --- | --- |
| Package layout | Flat (`counter/`-style) vs. layered (`status/`/`toggle/`-style) | Flat | Brief explicitly requires the small/`counter`-style shape; no layering requirement for this feature |
| `value` trim/blank/length ownership | (a) `@Size` on the DTO, (b) service-level trim-then-validate | (b) service | `@Size` measures the raw, untrimmed string — wrong per §2; mirrors `StatusService`'s already-established invariant logic |
| Missing vs. blank/too-long split | Single service-level check for all `value` failures vs. splitting missing (`@NotNull`) from blank/too-long (service) | Split | Missing already flows through the app-wide handler with zero new code; only the trim-dependent rule needs new logic — same reasoning as `named-status` |
| `{name}` validation | Reuse `validation.NameValidationConstants` vs. new feature-local constant | New feature-local constant | Brief's pattern is identical to `CounterNameConstants`'s rule but that class is package-private — cross-feature reuse isn't available, and one-constant-per-feature is the established convention (`CounterNameConstants`, `ToggleNameConstants`) |
| Not-found / value-invalid exceptions | Define in controller vs. define alongside `LabelService` (same package either way, since the package is flat) | Same package, thrown from `LabelService` | No dependency-direction concern here (flat package) — just keep them next to the code that raises them, matching `CounterNotFoundException` |

- No open questions — brief marks them resolved (in-memory only, GET
  unknown is `404`, no list/delete).

## 5. Implementation handoff

- New flat package `src/main/java/com/example/scratch/label/`:
  `LabelController`, `LabelRequest`, `LabelResponse`, `LabelService`,
  `LabelNameConstants`, `LabelNotFoundException`,
  `LabelValueInvalidException`, `LabelExceptionHandler`. No changes to
  `greeting/`, `farewell/`, `counter/`, `notes/`, `toggle/`, `status/`, or
  `validation/`.
- Impl plan must cover: PUT create-or-replace test, GET happy-path test,
  overwrite test, unknown-GET → `404` test, invalid-`{name}` → `400` test,
  missing-`value` → `400` test, blank/too-long-`value`-after-trim → `400`
  test, and one `/health` regression test.
- README: short **Labels** section alongside the existing counter/toggle/
  status entries.

Suggested branch: `feature/named-label` (already created).
