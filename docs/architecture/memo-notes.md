# Architecture & Design — Memo Notes API

## 1. Context

- **Problem:** The service exposes `GET /health`, greetings, farewells, and
  counters — all single-package, controller+service features. We need a
  fourth resource, an in-process memo note (`POST /api/v1/notes`,
  `GET /api/v1/notes/{id}`), built with a genuine **layered architecture**
  (web / application / domain / infrastructure, dependencies inward-only).
  This is explicitly a deliberate architectural exercise, not a scale need.
- **Goals:** Create-and-read a note with server-generated UUID identity and a
  trimmed, length-bounded text invariant; reuse the existing `400`/`404`
  envelope shapes; make the four layer boundaries real and enforced (a test
  proves Web cannot import Infrastructure), not just a comment.
- **Non-goals:** Persistence, auth, list/update/delete, tags/author/pagination,
  client-supplied ids, refactoring greeting/farewell/counter into layers,
  rate limiting, OpenAPI, a second request-size check.
- **Constraints:** Stay on Java 21 + Spring Boot `4.1.0`; four packages named
  so the layers are visible (`notes.web` / `notes.application` /
  `notes.domain` / `notes.infrastructure`); reuse the existing `400`
  (`validation_failed`) and counter-established `404` (`not_found`) envelope
  **shapes** — do not invent a third error document type.
- **Sources used:** `docs/briefs/memo-notes.md`; existing code (`greeting/`,
  `counter/` packages — `ValidationErrorResponse`/`FieldErrorDetail` record
  types, `CounterExceptionHandler` as the precedent for a feature-scoped
  advice); `docs/architecture/named-counter.md` (prior decisions, and its
  flagged debt item — see §5).

## 2. Current state (as evidenced)

- All three existing features are single-package: a thin `@RestController`
  delegates directly to a `@Service` that also holds any state, with request
  validation done via Bean Validation annotations on the request DTO. There
  is no precedent in this repo for a use-case/domain/port split — this
  feature introduces that shape for the first time, deliberately.
- The `400 validation_failed` envelope (`greeting.ValidationErrorResponse` /
  `greeting.FieldErrorDetail`) and the `404 not_found` envelope (same record
  types, different `error` value, established by `counter.CounterExceptionHandler`)
  are both already reusable **types**, independent of which advice class
  produces them. Farewell and counter both already import these types
  cross-package without depending on `greeting`'s validation *logic*.
- `docs/architecture/named-counter.md` §6 already flagged that the shared,
  app-wide `GreetingValidationExceptionHandler` was backing three unrelated
  features while still living under `greeting`, and said a rename/relocation
  was "worth doing before a fourth feature makes the misnomer worse." This
  is that fourth feature — see §5 for how this design resolves it without a
  rename.
- `RequestSizeLimitFilter` is registered with no path restriction, so the new
  `POST /api/v1/notes` is already covered by the 4096-byte cap with no change
  (per the brief, no second size check is added).

## 3. Recommended architecture

### 3.1 Components and responsibilities

| Layer / package | Responsibility | Must not |
| --- | --- | --- |
| **Web** (`notes.web`) | HTTP: routes, request/response JSON DTOs, status codes, mapping raw input to/from the Application layer; a small, notes-scoped error advice mapping domain exceptions to the `400`/`404` envelopes | Hold the note store, or the UUID/text invariant rules themselves |
| **Application** (`notes.application`) | Orchestrates the two use cases (create, get): calls Domain to construct/validate a note, calls the repository port to persist/find, returns the Domain result outward | Depend on Servlet/Spring Web types |
| **Domain** (`notes.domain`) | The `Note` concept: identity generation, the text invariant (trim, 1–200 chars), the "not found" signal, and the repository **port** (interface) that Infrastructure must implement | Depend on Spring Web or any concrete storage type |
| **Infrastructure** (`notes.infrastructure`) | Implements the Domain-defined repository port with an in-process map | Be called directly from Web, bypassing Application |
| `GreetingController`/`FarewellController`/`CounterController` and their packages | Unchanged | — |

### 3.2 Interactions and data flow

- **Create happy path:** Client sends `POST /api/v1/notes` with
  `{ "text": "..." }` → Web parses the JSON DTO (no Bean Validation
  annotations — see §5) and passes the raw string to Application → Application
  asks Domain to construct a new note (Domain generates the UUID identity and
  enforces the trim/length invariant as part of construction) → Application
  calls the repository port to persist it → Web maps the returned note to the
  `201` JSON shape (`id`, trimmed `text`, `createdAt`).
- **Get happy path:** Client sends `GET /api/v1/notes/{id}` → Web passes the
  raw `{id}` string to Application → Application asks Domain to parse it as a
  note identity (Domain enforces the UUID-format invariant) → Application
  calls the repository port; if present, returns the note; if absent, signals
  "not found" → Web maps to `200` or `404` accordingly.
- **Invalid `text` (blank after trim, or over 200 chars):** Domain's
  construction step rejects it → the notes-scoped advice maps it to `400`
  with the standard `validation_failed` envelope, field `"text"`.
- **Malformed `{id}` (not a UUID):** Domain's identity-parsing step rejects
  it → same advice maps it to `400`, field `"id"`.
- **Unknown but well-formed `{id}`:** Application's "not found" signal → the
  advice maps it to `404` with the counter-established `not_found` envelope,
  field `"id"`, message "note does not exist".
- **Malformed JSON body:** Already covered, unchanged — the existing
  app-wide `GreetingValidationExceptionHandler.handleMalformedBody` fires
  for any controller's unreadable body, notes included, with no new code.
- **Wrong HTTP method:** Falls through to Spring's default handling, per
  the brief.
- **Oversized body:** Already intercepted by `RequestSizeLimitFilter`.

### 3.3 Trust boundaries and data

- **Data classes:** A caller-supplied text string (bounded 1–200 chars after
  trim) and a server-generated UUID identity — no sensitive data, no
  persistence, lost on restart by design, same posture as counters.
- **Authn/authz:** None, consistent with the rest of this unauthenticated
  pilot service and the brief's non-goals.
- **Input handling:** Both external inputs (`text`, `{id}`) are validated at
  the Domain boundary before a `Note` can exist or be looked up — invalid
  input never reaches the repository or gets echoed unvalidated.
- **Resource exhaustion:** Same accepted, already-documented posture as
  counters (`docs/architecture/named-counter.md` §3.3/§6): an unauthenticated
  caller can create unboundedly many notes, each a small fixed-size entry.
  Not re-litigated here — same trade-off, same brief-scoped non-goal
  (persistence/cap/rate-limiting all explicitly out of scope for this slice).

## 4. Quality attributes

| Attribute | Target / strategy |
| --- | --- |
| Reliability | No external dependencies; in-memory, intentionally volatile |
| Maintainability | Layer boundaries are compiler-enforced by package structure and inward-only imports, plus a dedicated boundary test (§7) |
| Observability | None added at this scope, matching existing precedent |
| Performance | O(1) in-memory lookup/insert; no I/O |
| Security | Same input-bounding posture as the rest of the service; unbounded store growth accepted per non-goals (§3.3) |

## 5. Decisions

| Decision | Options considered | Choice | Rationale |
| --- | --- | --- | --- |
| Where the `400`/`404` error mapping for notes lives | Extend the shared `GreetingValidationExceptionHandler` a fourth time vs. a new, small advice scoped to the notes controller | New advice in `notes.web`, scoped via `assignableTypes` | Notes' failures are custom domain exceptions (`InvalidNoteTextException`, `InvalidNoteIdException`, `NoteNotFoundException`), not the Bean-Validation exception types the shared advice already handles — a poor fit for "just another case" on that class. This also resolves the debt flagged in `docs/architecture/named-counter.md` §6 (a fourth feature would have made the shared advice's misnomer worse) without a disruptive rename of already-shipped code. The `400`/`404` envelope **types** (`ValidationErrorResponse`/`FieldErrorDetail`) are still reused, satisfying the brief's "do not invent a third error document type." |
| Where the text/id invariants are enforced | Bean Validation annotations on the Web DTO (existing repo convention) vs. Domain value-object construction | Domain (`NoteText`, `NoteId` construction) | The brief requires a genuine Domain layer that "owns" identity + text invariants, and "trim, then check length" is a rule that a static annotation on the raw field can't express cleanly. This is an intentional divergence from greeting/farewell/counter's convention for this one feature, not an oversight. |
| Repository port ownership | Port interface defined in Application vs. in Domain | Domain (`notes.domain.NoteRepository`) | The port represents "notes can be found and saved," a concept belonging to the `Note` aggregate itself; Application then only orchestrates (construct via Domain, call the port), with no storage-shaped types of its own. Infrastructure implements this Domain-defined port — the required inward dependency direction. |
| Application → Web result shape | A dedicated Application-layer DTO vs. Application returns the Domain `Note` object directly | Application returns `Note` directly; Web maps it to the JSON response | `Note` carries no framework or storage detail — passing a Domain entity outward through the Application boundary for a read is normal in this style of architecture. A duplicate Application-only DTO with an identical shape would be a needless layer for a two-operation feature. |
| Identity generation | Web/Application generates the UUID vs. Domain generates it as part of construction | Domain (`Note` construction generates `NoteId`) | "Identity" is explicitly a Domain responsibility per the brief's own layer table; keeping generation there means no other layer can accidentally construct a note with a caller-supplied or missing id. |
| Layering-boundary enforcement mechanism | Add ArchUnit as a new test dependency vs. a small, dependency-free source-scan test | Dependency-free source-scan test (asserts no `.java` file under `notes.web` contains an `import` of `notes.infrastructure`) | The brief asks for exactly one narrow rule. ArchUnit is the standard tool for broader architecture-fitness suites, but adding a new test dependency for a single check is more than this slice needs; a source-scan test is easy to read, needs nothing new in `pom.xml`, and checks the literal rule the brief states ("no compile-time import from `notes.web` to `notes.infrastructure`"). Revisit if more layering rules accumulate. |
| Use-case invocation shape | Command objects per use case (CQRS-style) vs. plain method parameters on one Application service | Plain parameters on one `NoteApplicationService` (`createNote(String rawText)`, `getNote(String rawId)`) | Two simple use cases don't justify command-object ceremony; avoids a premature abstraction for this slice's actual size. |

## 6. Open questions

- None blocking. Two deliberate, explicitly-flagged trade-offs carried into
  implementation:
  1. Web depends directly on Domain types for two things — the
     Application-returned `Note` (to map to JSON) and the Domain exception
     types (to catch them in the notes-scoped advice). The brief's only
     stated "must not" for Web is holding the store or the invariant rules,
     and the required boundary test only forbids Web→Infrastructure — so
     this is within the brief's letter, and is standard practice for this
     architecture style (entities crossing the Application boundary on
     reads; exception vocabulary being part of a layer's public contract).
  2. `notes.web` imports `greeting.ValidationErrorResponse`/`FieldErrorDetail`
     for its error envelope — a compile-time dependency on an unrelated
     feature package, from the one feature in this repo that otherwise
     argues for strict module boundaries (`NotesLayeringTest`). This mirrors
     the existing `counter.CounterExceptionHandler` precedent and is
     required by the brief ("reuse the existing `400`... do not invent a
     third error document type"), so it isn't addressed by a code change
     here. `NotesLayeringTest` deliberately only checks
     `notes.web ↛ notes.infrastructure`, not cross-feature imports, so this
     dependency is invisible to it by design, not by oversight. A future
     extraction of these two record types into a neutral shared package
     (e.g. `com.example.scratch.web`/`errors`) would remove this for
     `greeting`, `counter`, and `notes` at once — worth doing if a fourth
     consumer appears, same pattern as the debt already flagged in
     `docs/architecture/named-counter.md` §6.

## 7. Implementation implications

- Add four new packages (main + test where applicable):
  `notes.web`, `notes.application`, `notes.domain`, `notes.infrastructure`.
- **Domain:** `Note` (aggregate: id, text, createdAt), `NoteId` (UUID value
  object — parses/generates, throws `InvalidNoteIdException` on a malformed
  string), `NoteText` (value object — trims, enforces 1–200 chars, throws
  `InvalidNoteTextException`), `NoteRepository` (port: save + find-by-id),
  `NoteNotFoundException`.
- **Infrastructure:** one Spring-managed bean implementing `NoteRepository`
  with a `ConcurrentHashMap`-backed store (same atomic-per-key precedent as
  `counter.CounterService`, though notes' create path doesn't need
  create-or-increment semantics — a plain `put`/`get` suffices since ids are
  server-generated and unique by construction).
- **Application:** one Spring-managed service with `createNote(String)` and
  `getNote(String)`, depending only on the `NoteRepository` port and Domain
  types.
- **Web:** a thin controller for `POST /api/v1/notes` and
  `GET /api/v1/notes/{id}`, a request DTO (`text`), a response DTO
  (`id`, `text`, `createdAt`), and the notes-scoped advice mapping the three
  Domain exceptions to the `400`/`404` envelopes (reusing
  `greeting.ValidationErrorResponse`/`FieldErrorDetail`).
- **Boundary test:** a source-scan test (no new dependency) asserting no
  `.java` file under `notes.web` imports `notes.infrastructure`.
- Tests per the brief's coverage list: create → `201` + trimmed text + UUID
  id; get after create → `200` same body; blank/too-long text → `400`;
  malformed id → `400`; unknown id → `404`; the layering test above;
  regression checks that health/greetings/farewells/counters are unaffected.
- README gets a brief new section (methods, paths, example), alongside the
  existing sections.

## 8. Addendum — deep-review hardening

`.deep-review/2026-08-14-feature-memo-notes/REPORT.md` raised 6 P2s (no
P0/P1); 2 were actionable and fixed before push, 4 were informational or
already-accepted (unbounded growth, narrow layering-test scope, no `{id}`
pre-check, no `text` character-class restriction — see the report for
detail):

- `NoteId.fromString` now also catches a `null` input (previously only
  `IllegalArgumentException`, so `UUID.fromString(null)`'s
  `NullPointerException` would have fallen through every handler to a
  default `500`). Not reachable via the live `GET /api/v1/notes/{id}`
  endpoint today, but closes the gap in the one method this document claims
  fully owns the id invariant.
- **Deliberately did not** also chain the caught `IllegalArgumentException`
  as `InvalidNoteIdException`'s cause, despite that being the deep-review
  finding's suggested fix. Trying it first surfaced a real, previously
  undetected cross-feature bug: Spring's `@ExceptionHandler` resolution
  falls back to an exception's cause chain when no handler matches the
  exception's own type, and `greeting.GreetingValidationExceptionHandler`
  already has an app-wide `@ExceptionHandler(IllegalArgumentException.class)`
  (added for locale validation). Chaining the cause made that unrelated
  handler intercept `InvalidNoteIdException` instead of
  `notes.web.NoteExceptionHandler`, silently changing the `400` response's
  `field` from `"id"` to `"locale"` — caught immediately by
  `NoteControllerTest`, not shipped. `NoteId.java`'s Javadoc now documents
  this landmine in place; `NoteIdTest` asserts `.hasNoCause()` as a
  regression guard.
- Documented (§6) the `notes.web → greeting` type dependency as an
  explicitly accepted trade-off, matching the `counter` precedent and the
  brief's explicit reuse requirement — no code change, since the boundary
  test is deliberately scoped to the one edge the brief requires enforced.

No architecture decision from §5 changed; this is hardening within the
already-agreed shape, not a Decision Drift.
