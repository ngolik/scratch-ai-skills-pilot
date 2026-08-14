# Brief: Named in-memory counter

## Goal

Add a small HTTP API so a client can increment and read a **named integer
counter** stored only in process memory. This is a different kind of feature
from greetings/farewells (no localized messages, no `locale`).

Keep `GET /health`, `POST /api/v1/greetings`, and `POST /api/v1/farewells`
unchanged.

## In scope

1. **Counter name** (path segment `{name}`):
   - required, 1–40 characters
   - lowercase ASCII letters, digits, and hyphen only (`^[a-z0-9-]{1,40}$`)
   - must not start or end with a hyphen; must not contain `--`
   - invalid name → `400` with the **same** JSON problem body as greetings
     (field `"name"`)

2. **`POST /api/v1/counters/{name}/increments`**
   - No request body required (empty body or omitted is fine)
   - If the counter does not exist, create it at `0` then add `1`
   - Success `200` with JSON:
     ```json
     {
       "name": "jobs",
       "value": 1,
       "updatedAt": "<ISO-8601 UTC timestamp>"
     }
     ```
   - Concurrent increments on the same name must not lose updates
     (in-process; no distributed lock)

3. **`GET /api/v1/counters/{name}`**
   - Success `200` with the same JSON shape (`updatedAt` = last increment time)
   - Unknown name → `404` with:
     ```json
     {
       "error": "not_found",
       "details": [
         { "field": "name", "message": "counter does not exist" }
       ]
     }
     ```

4. **Wrong HTTP method** on these paths may use framework defaults.

5. Existing **413 payload-too-large** filter (4096 bytes) already applies
   globally; do not add a second size check.

## Out of scope / non-goals

- Persistence / DB / Redis — counters are lost on process restart
- Auth / API keys
- Decrement, reset, list-all, TTL, or max-value cap
- Increment by an arbitrary delta (always `+1`)
- Rate limiting
- Changing `/health`, greetings, or farewells
- Reusing greeting **message templates** or locale enums
- Deployment / CI beyond what already exists
- OpenAPI/Swagger UI (document the contract in architecture + README only)

## Constraints

- Stay on the **existing** Java 21 + Spring Boot 4.1.0 stack already in this repo
  (do **not** silently change major Boot version; if a change is required,
  update architecture + impl-plan in the same change — Decision Drift).
- Prefer a thin controller + small service; keep the in-memory store behind
  the service (not in the controller).
- Counter **name** rules are **not** the person-name rules in
  `NameValidationConstants` — do not overload that class for this feature.
- Reuse the existing global `400` `validation_failed` JSON shape where it fits;
  add a small `404` mapping for unknown counters (same `error` + `details`
  envelope, different `error` code).
- Unit / WebMvc tests required for:
  - first increment creates and returns `value: 1`
  - second increment returns `value: 2`
  - GET after increment returns the same value
  - GET unknown name → `404` with the body above
  - invalid name (uppercase, leading hyphen, too long) → `400`
  - greetings, farewells, and health still work (regression)

## Unchanged contracts

- `GET /health` — `200`, `text/plain`, body `ok`
- `POST /api/v1/greetings` — greeting JSON; same validation/error shape
- `POST /api/v1/farewells` — farewell JSON; same validation/error shape
- `413` oversized body (`payload_too_large`, max 4096 bytes) for POST endpoints

## Acceptance criteria

- [ ] `POST /api/v1/counters/{name}/increments` creates-or-increments and returns `200` + JSON above
- [ ] Two increments on the same name yield `1` then `2` (no lost update in one JVM)
- [ ] `GET /api/v1/counters/{name}` returns `200` for an existing counter and `404` for a missing one
- [ ] Invalid `{name}` returns `400` with `validation_failed`
- [ ] `GET /health`, greetings, and farewells still match their current contracts
- [ ] Tests cover happy paths, key validation/`404` cases, and regressions, and pass via project build
- [ ] Architecture + impl-plan docs exist under `docs/` for this feature
- [ ] README mentions the new endpoints briefly (methods, paths, example)

## Open questions

None — in-memory only; restart clears counters. No list-all in this slice.

## Sources

- Pasted product intent in chat (pilot follow-up; **not** another greeting/farewell)
- Existing contracts: `README.md`, `docs/briefs/echo-greeting.md`, `docs/briefs/echo-farewell.md`

## Suggested slug

`named-counter`
