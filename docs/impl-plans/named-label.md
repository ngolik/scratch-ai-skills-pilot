# Implementation Plan — named-label

## Summary

- **Goal:** Add `PUT`/`GET /api/v1/labels/{name}` for a named in-memory
  label value, in one flat package (`label/`, `counter/`-style, no classic
  layers), reusing existing `400 validation_failed` / `404 not_found`
  shapes.
- **In scope:** new `label` package, its tests, a brief README update.
- **Out of scope:** persistence, auth, list/delete/TTL, classic Spring
  layers, changes to `greeting`/`farewell`/`counter`/`notes`/`toggle`/
  `status`/`validation`.
- **Architecture input:** `docs/architecture/named-label.md`

## Touch map

| Area | Why |
| --- | --- |
| `src/main/java/com/example/scratch/label/` (new) | `LabelController`, `LabelRequest`, `LabelResponse`, `LabelService`, `LabelNameConstants`, `LabelNotFoundException`, `LabelValueInvalidException`, `LabelExceptionHandler` |
| `src/test/java/com/example/scratch/label/` (new) | Unit + WebMvc tests |
| `README.md` | Document new endpoints |

Reference-only (no changes): `counter/*` (flat-package pattern mirrored),
`status/service/StatusService.java` (trim/blank/max-length invariant
precedent — logic mirrored in `LabelService`), `greeting/
GreetingValidationExceptionHandler.java`, `greeting/ValidationErrorResponse.java`,
`greeting/FieldErrorDetail.java` (reused, not modified).

## Steps

### Step 1 — DTOs, name constant, exceptions

- **Outcome:** Request/response shapes, the label name pattern, and both
  domain exceptions exist, matching the brief's JSON contract and regex.
- **Approach:**
  - `label.LabelRequest` — record with `String value`
    (`@NotNull(message = "must not be blank")` — catches missing/null;
    missing-field JSON binds to `null`, fails Bean Validation → existing
    app-wide `400` handler, field `"value"`). No `@Size`/`@Pattern` here —
    blank/length is checked only after trim, in the service.
  - `label.LabelResponse` — record: `name`, `value`, `updatedAt` (`Instant`).
  - `label.LabelNameConstants` — package-private constant class holding
    `^(?!.*--)[a-z0-9]([a-z0-9-]{0,38}[a-z0-9])?$` (identical rule to
    `CounterNameConstants`; brief: 1–40 chars, lowercase/digits/hyphens, no
    leading/trailing hyphen, no `--`). Do **not** reuse
    `validation.NameValidationConstants` or `counter.CounterNameConstants`
    (package-private, not importable).
  - `label.LabelNotFoundException` — unchecked, mirrors
    `CounterNotFoundException`.
  - `label.LabelValueInvalidException` — unchecked, mirrors
    `StatusMessageInvalidException`, message carries the specific reason
    (`"must not be blank"` / `"must be at most 32 characters"`).
- **Tests:** None standalone (records/constants/exceptions); covered by
  Step 3/4 WebMvc tests.
- **Done when:** Types compile; reviewed against brief's JSON shapes.

### Step 2 — Service (value trim/blank/length invariant, atomic upsert)

- **Outcome:** `label.LabelService` implements create-or-replace and get,
  trims and validates `value` content, signals not-found on a missing
  `GET`.
- **Approach:**
  - `ConcurrentMap<String, LabelResponse> labels = new ConcurrentHashMap<>()`
    (same field shape as `CounterService`).
  - `setLabel(String name, String rawValue)`:
    - trim `rawValue`; if empty → throw
      `LabelValueInvalidException("must not be blank")`.
    - if trimmed length > 32 → throw
      `LabelValueInvalidException("must be at most 32 characters")`.
    - `labels.compute(name, (key, existing) -> new LabelResponse(key, trimmedValue, Instant.now()))`
      — atomic create-or-replace per key, same pattern as
      `CounterService.increment`.
  - `getLabel(String name)`: `labels.get(name)`; if `null` → throw
    `LabelNotFoundException`; else return it.
- **Tests:** Unit tests for `LabelService` (no mocking needed): create-then-get
  returns the same trimmed `value`; overwrite reflects the new trimmed
  `value` on next get; get-unknown raises `LabelNotFoundException`;
  `" beta "` → stored/returned as `"beta"`; blank (`"   "`) raises
  `LabelValueInvalidException`; 33-char trimmed value raises
  `LabelValueInvalidException`; 34 raw chars that trim to 32 succeeds
  (raw-vs-trimmed-length distinction from architecture doc §2).
- **Done when:** Service unit tests pass, including the trim-boundary cases
  above.

### Step 3 — Controller + exception mapping (vertical slice complete)

- **Outcome:** `PUT`/`GET /api/v1/labels/{name}` are live end-to-end with
  `200`/`400`/`404` behavior.
- **Approach:**
  - `label.LabelController`, `@RestController @Validated` (class-level
    `@Validated` routes `@PathVariable @Pattern` violations through
    `ConstraintViolationException`, caught by the existing app-wide
    handler — same reasoning as `CounterController`).
    - `@PutMapping("/api/v1/labels/{name}")` — `@PathVariable @Pattern(regexp = LabelNameConstants.NAME_PATTERN)` name, `@RequestBody @Valid LabelRequest` → `service.setLabel(...)` → `200`.
    - `@GetMapping("/api/v1/labels/{name}")` — same `{name}` validation → `service.getLabel(...)` → `200`, or propagate not-found.
  - `label.LabelExceptionHandler` — `@RestControllerAdvice(assignableTypes = LabelController.class)`, mirroring `CounterExceptionHandler`, with **two** handlers:
    - `LabelNotFoundException` → `404`, `greeting.ValidationErrorResponse("not_found", [FieldErrorDetail("name", "label does not exist")])`.
    - `LabelValueInvalidException` → `400`, `greeting.ValidationErrorResponse("validation_failed", [FieldErrorDetail("value", exception.getMessage())])`.
- **Tests:** WebMvc tests (`MockMvc`, matching existing test style):
  - `PUT` create → `GET` → same `value`.
  - `PUT` overwrite → `GET` reflects the new trimmed `value`.
  - `GET` unknown name → `404` with the `not_found` envelope.
- **Done when:** WebMvc tests above pass.

### Step 4 — Validation and regression coverage

- **Outcome:** Full brief test-requirements list is covered.
- **Approach:** Add/confirm tests for:
  - Invalid `{name}` (bad chars, too long, leading/trailing hyphen, `--`) →
    `400 validation_failed`, field `"name"`.
  - Missing `value` → `400 validation_failed`, field `"value"` (via
    `@NotNull`).
  - Blank `value` (whitespace-only) → `400 validation_failed`, field
    `"value"` (via `LabelValueInvalidException`).
  - Too-long `value` (trimmed length > 32) → `400 validation_failed`,
    field `"value"`.
  - Both `400` paths (DTO-level missing vs. service-level blank/too-long)
    must produce the **identical** envelope shape — assert this
    explicitly, since they come from two different exception handlers.
  - Regression: `GET /health` still returns `ok` (per brief's minimum
    regression requirement).
- **Tests:** As listed above.
- **Done when:** `mvn test` passes for the full suite, including the new
  label tests and all pre-existing tests.

### Step 5 — README update

- **Outcome:** `README.md` briefly documents the new endpoints.
- **Approach:** Add `PUT`/`GET /api/v1/labels/{name}` next to the existing
  endpoint list, with method, path, and one example request/response —
  same level of detail as the existing counter/toggle/status entries.
- **Tests:** None (docs only).
- **Done when:** README change reviewed for consistency with existing
  entries.

## Risks and mitigations

| Risk | Mitigation |
| --- | --- |
| Path-variable `@Pattern` validation silently falls back to Spring's native `HandlerMethodValidationException` instead of `ConstraintViolationException` (breaking the `400` shape) | Follow `CounterController`'s exact pattern: class-level `@Validated` + `@PathVariable @Pattern`, verified by the existing app-wide handler's `ConstraintViolationException` branch; Step 4 adds a name-validation test to catch a regression immediately |
| Two independent `400` code paths for `value` (DTO `@NotNull` for missing vs. service-raised `LabelValueInvalidException` for blank/too-long) drift into different JSON shapes over time | Both funnel through `greeting.ValidationErrorResponse`/`FieldErrorDetail` with field `"value"`; Step 4 adds an explicit test asserting both produce the same envelope shape |
| `@Size`-style raw-length validation accidentally applied to `value` on the DTO, silently rejecting valid inputs that are long-but-trim-short (or the reverse) | Do **not** add any `@Size`/`@Length` annotation to `LabelRequest.value`; length is checked only after trim, only in `LabelService` (architecture doc §2, §3) |
| `compute`-based upsert accidentally re-validates or drops the trimmed value on overwrite | `setLabel` trims/validates once, then passes the already-trimmed value into the `compute` lambda — no re-derivation inside the lambda |

## Open questions

None — brief and architecture doc left no open items.
