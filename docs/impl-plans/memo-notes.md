# Implementation Plan — Memo Notes API

## Summary

- **Goal:** Add `POST /api/v1/notes` (create) and `GET /api/v1/notes/{id}`
  (read), built as a genuine four-layer feature (web / application / domain /
  infrastructure, dependencies inward-only), reusing the existing `400`/`404`
  envelope shapes — without changing health, greetings, farewells, or
  counters.
- **In scope:** `notes.domain` (`Note`, `NoteId`, `NoteText`, `NoteRepository`
  port, three domain exceptions), `notes.infrastructure` (in-memory
  repository adapter), `notes.application` (one orchestration service),
  `notes.web` (controller, DTOs, a notes-scoped error advice), a
  dependency-free layering-boundary test, unit/WebMvc tests, README update.
- **Out of scope:** Persistence, auth, list/update/delete, client-supplied
  ids, refactoring existing features into layers, rate limiting, OpenAPI, a
  second request-size check, ArchUnit (per architecture doc §5).
- **Architecture input:** `docs/architecture/memo-notes.md`

## Touch map

| Area | Why |
| --- | --- |
| `src/main/java/com/example/scratch/notes/domain/` (new) | `Note`, `NoteId`, `NoteText`, `NoteRepository` port, `InvalidNoteIdException`, `InvalidNoteTextException`, `NoteNotFoundException` |
| `src/main/java/com/example/scratch/notes/infrastructure/` (new) | `InMemoryNoteRepository` implementing the port |
| `src/main/java/com/example/scratch/notes/application/` (new) | `NoteApplicationService` (createNote/getNote orchestration) |
| `src/main/java/com/example/scratch/notes/web/` (new) | `NoteController`, `CreateNoteRequest`, `NoteResponse`, `NoteExceptionHandler` |
| `src/test/java/com/example/scratch/notes/` (new, mirrors main) | Unit tests per layer + one layering-boundary test at the `notes` root |
| `src/test/java/com/example/scratch/{greeting,farewell,counter}/`, `HealthControllerTest` | No code changes; re-run as regression evidence |
| `README.md` | Add a brief new section (methods, paths, example) |

No changes to `pom.xml` — no new dependency (architecture doc §5 rejects
ArchUnit in favor of a dependency-free source-scan test). No changes to
`greeting`/`farewell`/`counter` packages; `notes.web` only imports the
existing `greeting.ValidationErrorResponse`/`FieldErrorDetail` **types** for
its own advice, matching the farewell/counter precedent.

## Steps

### Step 1 — Domain layer (no Spring, no HTTP)

- **Outcome:** `Note`, `NoteId`, `NoteText` exist with their invariants
  enforced at construction; `NoteRepository` port and the three domain
  exceptions exist. No Application/Web/Infrastructure code yet.
- **Approach:**
  - `NoteText` (record wrapping the trimmed `String value`): compact
    constructor trims the input and throws `InvalidNoteTextException` if
    null/blank-after-trim or over 200 characters, so `text()` is always
    already the trimmed value.
  - `NoteId` (record wrapping `UUID value`): `NoteId.newId()` generates a
    fresh id; `NoteId.fromString(String raw)` wraps `UUID.fromString`,
    catching its `IllegalArgumentException` and rethrowing
    `InvalidNoteIdException`.
  - `Note` (record: `id`, `text`, `createdAt`): static factory
    `Note.create(NoteText text, Instant createdAt)` generates the id via
    `NoteId.newId()` — identity generation is a Domain responsibility, per
    architecture §5.
  - `NoteRepository` (interface): `save(Note)`, `findById(NoteId): Optional<Note>`.
  - `InvalidNoteTextException`, `InvalidNoteIdException`, `NoteNotFoundException`:
    plain `RuntimeException` subclasses, no Spring types.
- **Tests:** Direct unit tests (no Spring context): `NoteText` — valid text
  is trimmed; blank/whitespace-only throws; exactly 200 chars passes; 201
  chars throws; null throws. `NoteId` — valid UUID string parses; malformed
  string throws `InvalidNoteIdException`; `newId()` produces distinct ids
  across calls. `Note.create` — produces a `Note` with a fresh id, the given
  text, and the given timestamp.
- **Done when:** All the above pass with no Spring context required.

### Step 2 — Infrastructure layer

- **Outcome:** `InMemoryNoteRepository` correctly implements the
  `NoteRepository` port.
- **Approach:** `@Repository`-annotated class backed by a
  `ConcurrentHashMap<NoteId, Note>`; `save` puts, `findById` returns
  `Optional.ofNullable(map.get(id))`. Plain `put`/`get` suffices — unlike
  `counter.CounterService`, notes don't need create-or-increment semantics
  (ids are unique by construction).
- **Tests:** Direct unit test against a fresh `InMemoryNoteRepository`
  instance (no Spring context): save-then-findById round-trips the same
  note; findById on a never-saved id returns `Optional.empty()`.
- **Done when:** Both cases pass with no Spring context required.

### Step 3 — Application layer

- **Outcome:** `NoteApplicationService.createNote(String)` and `.getNote(String)`
  correctly orchestrate Domain construction/parsing and the repository port,
  with no HTTP concerns.
- **Approach:** `@Service`-annotated class depending only on `NoteRepository`
  (constructor-injected). `createNote(rawText)`: builds `new NoteText(rawText)`,
  wraps it in `Note.create(text, Instant.now())`, calls `repository.save(note)`,
  returns the note. `getNote(rawId)`: calls `NoteId.fromString(rawId)`, then
  `repository.findById(id).orElseThrow(() -> new NoteNotFoundException(id))`.
- **Tests:** Unit tests with a **mocked** `NoteRepository` (Mockito, already
  on the test classpath per the farewell/counter precedent) to isolate
  orchestration from storage correctness (already covered in Step 2):
  `createNote` with valid text calls `save` once and returns a note with the
  trimmed text; `createNote` with invalid text throws
  `InvalidNoteTextException` and never calls `save` (verify no interaction);
  `getNote` with a malformed id throws `InvalidNoteIdException` and never
  calls the repository; `getNote` with a well-formed but unknown id throws
  `NoteNotFoundException`; `getNote` with a known id returns the stored note.
- **Done when:** All five cases pass; no Spring context, no HTTP types
  anywhere in this package (self-check before moving on).

### Step 4 — Web layer and the notes-scoped `400`/`404` advice

- **Outcome:** `POST /api/v1/notes` returns `201` with the trimmed text and a
  UUID id; `GET /api/v1/notes/{id}` returns `200`/`404`; invalid text/id
  return `400`, both via the standard envelope shapes.
- **Approach:**
  - `CreateNoteRequest` (record: `text`), `NoteResponse` (record: `id`
    as `String`, `text`, `createdAt` as `Instant`) — no Bean Validation
    annotations (architecture §5 — invariants live in Domain).
  - `NoteController`: `@RestController`, `POST /api/v1/notes` (`@ResponseStatus(CREATED)`,
    plain `@RequestBody`, no `@Valid`) and `GET /api/v1/notes/{id}`
    (plain `@PathVariable String id`, no `@Pattern`); both delegate to
    `NoteApplicationService` and map the returned `Note` to `NoteResponse`
    (`id().value().toString()`, `text().value()`, `createdAt()`).
  - `NoteExceptionHandler`: `@RestControllerAdvice(assignableTypes = NoteController.class)`
    mapping `InvalidNoteTextException`/`InvalidNoteIdException` to `400`
    (`validation_failed`, field `"text"`/`"id"`) and `NoteNotFoundException`
    to `404` (`not_found`, field `"id"`, message "note does not exist"),
    reusing `greeting.ValidationErrorResponse`/`FieldErrorDetail`.
- **Tests:** `@WebMvcTest(NoteController.class)` with the real Application/
  Domain/Infrastructure beans imported (not mocked — this is the
  integration point the brief's acceptance criteria target directly):
  create → `201`, trimmed text, `id` matches UUID format, `createdAt`
  present; get immediately after create (same test) → `200`, identical
  body; blank text → `400`/`validation_failed`/field `"text"`; 201-char
  text → `400` same shape; malformed id (e.g. `"not-a-uuid"`) → `400`/field
  `"id"`; well-formed but unknown id → `404`/`not_found`/field `"id"`/message
  "note does not exist". Each test uses a distinct note (created within its
  own test method) to avoid cross-test state leakage from the shared
  `@WebMvcTest` context, per the same pattern used in `CounterControllerTest`.
- **Done when:** All listed cases pass; existing `GreetingControllerTest`/
  `FarewellControllerTest`/`CounterControllerTest` still pass unmodified
  (proves the new advice's `assignableTypes` scoping doesn't interfere with
  the other controllers' error handling).

### Step 5 — Layering boundary test, regression, and README

- **Outcome:** The brief's required layering check exists and passes; full
  suite green; README documents the new endpoints.
- **Approach:**
  - Add one test (e.g. `notes.NotesLayeringTest`) that walks every `.java`
    file under `src/main/java/com/example/scratch/notes/web/` and asserts
    none contain the substring `com.example.scratch.notes.infrastructure`
    (source-text scan, per architecture §5 — no new dependency).
  - Run the full `mvn test` suite (not just `notes`) to confirm health/
    greetings/farewells/counters are unaffected.
  - Add a short "Notes API" README section (both methods/paths, one create
    example, one get example, and the `404` example) near the existing
    counter section.
- **Tests:** The layering test itself, plus the full `mvn test` run
  (regression evidence for G3). README is manual-review only.
- **Done when:** The layering test passes, the full suite passes, and a
  reader unfamiliar with the repo can `curl` both new endpoints using only
  the README.

## Risks and mitigations

| Risk | Mitigation |
| --- | --- |
| Domain invariant logic (trim-then-length-check) has an off-by-one at the 200-char boundary | Step 1 explicitly tests both the 200-char (pass) and 201-char (fail) boundary cases before any HTTP wiring exists |
| `NoteExceptionHandler`'s `assignableTypes` scoping is wrong and either misses `NoteController`'s exceptions (falling through to a default 500) or accidentally intercepts another controller's | Step 4's "done when" re-runs the other three controllers' existing tests unmodified, and asserts the exact `400`/`404` shape for every notes case — either failure mode would show up immediately |
| The source-scan layering test is fragile (e.g. misses an import written unusually, like a fully-qualified reference instead of an `import` statement) | Scoped narrowly to this slice's actual risk (a straightforward `import` in a small, freshly-written package); revisit with ArchUnit only if the check needs to get smarter later (architecture §5) |
| `NoteApplicationService` tests (mocked repository) pass but the real `InMemoryNoteRepository` wiring is subtly wrong (e.g. bean not picked up, wrong scope) | Step 4's WebMvc tests use the real Infrastructure bean end-to-end (not mocked), so a wiring mistake fails there even if Step 3's isolated tests are green |

## Open questions

- None blocking.
