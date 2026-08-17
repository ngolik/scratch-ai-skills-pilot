# Brief: Named in-memory toggle

## Goal

Add a small HTTP API so a client can **set and read a named boolean toggle**
stored only in process memory. This is a new resource (not a greeting, farewell,
counter, or memo note). Use **classic Spring layered layout** (controller, dto,
service, entity, repository, config).

Keep `GET /health`, greetings, farewells, and counters unchanged. Do not
implement `memo-notes` in this change even if that brief exists.

## In scope

1. **Toggle name** (path segment `{name}`):
   - required, 1–40 characters
   - lowercase ASCII letters, digits, and hyphen only (`^[a-z0-9-]{1,40}$`)
   - must not start or end with a hyphen; must not contain `--`
   - invalid name → `400` with the **same** JSON problem body as greetings
     (field `"name"`)

2. **`PUT /api/v1/toggles/{name}`**
   - Request body (JSON):
     ```json
     { "enabled": true }
     ```
   - `enabled` — required boolean (`true` / `false`); missing or non-boolean → `400`
     `validation_failed` (field `"enabled"`)
   - If the toggle does not exist, create it; if it exists, replace `enabled`
   - Success `200` with JSON:
     ```json
     {
       "name": "dark-mode",
       "enabled": true,
       "updatedAt": "<ISO-8601 UTC timestamp>"
     }
     ```

3. **`GET /api/v1/toggles/{name}`**
   - Success `200` with the same JSON shape
   - Unknown name → `404` with the **same envelope as counters**:
     ```json
     {
       "error": "not_found",
       "details": [
         { "field": "name", "message": "toggle does not exist" }
       ]
     }
     ```

4. **Wrong HTTP method** on these paths may use framework defaults.

5. Existing **413 payload-too-large** filter (4096 bytes) already applies
   globally; do not add a second size check.

## Out of scope / non-goals

- Persistence / DB / JPA / files — toggles are lost on process restart
  (repository is in-memory; **entity** is still required as the stored model)
- Auth / API keys
- List-all, delete, TTL, default-on-missing (GET unknown is `404`, not `enabled: false`)
- Implementing `memo-notes` or changing counters
- Refactoring existing features into this layout
- Hexagonal / ports-and-adapters packages
- Rate limiting, OpenAPI/Swagger UI
- Deployment / CI beyond what already exists

## Constraints

- Stay on the **existing** Java 21 + Spring Boot 4.1.0 stack already in this repo
  (do **not** silently change major Boot version; if a change is required,
  update architecture + impl-plan in the same change — Decision Drift).
- **Classic Spring layered architecture is required** for this feature.
  Separate types (and packages) for each of:

  | Layer | Responsibility |
  | --- | --- |
  | **controller** | HTTP routes, status codes; accept/return DTOs only |
  | **dto** | Request/response JSON (`SetToggleRequest`, `ToggleResponse`) |
  | **service** | Use cases: validate name, upsert/get, map entity ↔ dto |
  | **entity** | Stored toggle (`name`, `enabled`, `updatedAt`) — not a JSON DTO |
  | **repository** | In-memory save/findByName; no HTTP, no DTO |
  | **config** | Wires the in-memory repository (and any feature beans) |

  Dependency direction: **controller → service → repository**; controller must
  **not** call repository; dto must **not** leak into repository; entity must
  **not** be the HTTP response body.

  Package layout must make layers visible, for example:
  `…toggle.controller` / `…toggle.dto` / `…toggle.service` / `…toggle.entity` /
  `…toggle.repository` / `…toggle.config`.

  Architecture + impl-plan must name these layers. Do **not** flatten them
  into one package the way `greeting/` / `farewell/` / `counter/` are structured.
- Toggle **name** rules match counters, but do **not** overload
  `NameValidationConstants` (person names) or merge this into the counter
  package.
- Reuse the existing `400` `validation_failed` JSON shape; reuse the counter
  `404` `not_found` envelope. Do not invent a third error document type.
- **Doc-first:** architecture and impl-plan should cite this brief and
  `docs/ai-context/` — do not inventory unrelated packages.
- Unit / WebMvc tests required for:
  - PUT create then GET → same `enabled`
  - PUT overwrite `true` → `false` → GET reflects `false`
  - GET unknown name → `404`
  - invalid name / missing `enabled` → `400`
  - health, greetings, farewells, counters still work (regression)
  - a test or review check that **controller does not import repository**
    (and repository does not import dto)

## Unchanged contracts

- `GET /health` — `200`, `text/plain`, body `ok`
- `POST /api/v1/greetings` / `POST /api/v1/farewells` — existing JSON contracts
- Counter increment + get — existing JSON contracts
- `413` oversized body (`payload_too_large`, max 4096 bytes) for POST/PUT bodies

## Acceptance criteria

- [ ] `PUT /api/v1/toggles/{name}` creates-or-updates and returns `200` + JSON above
- [ ] `GET /api/v1/toggles/{name}` returns `200` for an existing toggle and `404` for a missing one
- [ ] Invalid `{name}` or invalid body return `400` with `validation_failed`
- [ ] Code is split into controller / dto / service / entity / repository / config with the dependency rules above
- [ ] Existing health, greeting, farewell, and counter contracts still pass
- [ ] Tests cover happy paths, validation/`404`, layering, and regressions, and pass via project build
- [ ] Architecture + impl-plan docs exist under `docs/` and describe these Spring layers
- [ ] README mentions the new endpoints briefly (methods, paths, example)

## Open questions

None — in-memory only; GET unknown is `404`. No list/delete.

## Sources

- Pasted product intent in chat (plugin 0.4.0 E2E: doc-first + default pre-push-review)
- Existing contracts: `README.md`, `docs/briefs/named-counter.md`
- Product context: `docs/ai-context/`

## Suggested slug

`named-toggle`
