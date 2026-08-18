# Brief: Named in-memory label (small)

## Goal

Add a **small** HTTP API so a client can set and read one named label in process
memory. Keep the change as thin as greetings/counters: one package, controller +
service. Do **not** introduce classic Spring layers for this task.

Keep existing HTTP contracts unchanged.

## In scope

1. **`PUT /api/v1/labels/{name}`**
   - `{name}`: 1–40 chars, `^[a-z0-9-]{1,40}$`, no leading/trailing hyphen, no `--`
   - Body: `{ "value": "beta" }`
   - `value`: required; trim; after trim length 1–32
   - Invalid name/`value` → `400` `validation_failed` (field `"name"` or `"value"`)
   - Create or replace. Success `200`:
     ```json
     { "name": "release-channel", "value": "beta", "updatedAt": "<ISO-8601 UTC>" }
     ```

2. **`GET /api/v1/labels/{name}`**
   - `200` same JSON, or `404` `not_found` with field `"name"`
     (same envelope as counters)

3. Reuse the existing 413 body-size filter. No extra size check.

## Out of scope / non-goals

- Persistence, auth, list, delete, TTL
- Classic layers (controller/dto/service/entity/repository/config packages)
- Hexagonal layout, OpenAPI, refactor of existing features
- Changing notes, toggles, statuses, or counters

## Constraints

- Stay on Java 21 + Spring Boot 4.1.0. Do not silently change majors.
- **Small-task shape:** one package `…label` with a controller and a small
  in-memory service (same style as `counter/`). No extra layer packages.
- Reuse existing `400` / `404` JSON envelopes.
- **Doc-first:** thin architecture note is enough (one recommendation + handoff).
  Do not scan the whole repo.
- Tests: PUT then GET; overwrite; unknown GET → `404`; invalid name/`value` →
  `400`; one regression that `/health` still returns `ok`.

## Unchanged contracts

- `GET /health`
- Greetings, farewells, counters, notes, toggles, statuses

## Acceptance criteria

- [ ] PUT create-or-update returns `200` with the JSON above
- [ ] GET returns `200` or `404` as specified
- [ ] Invalid input returns `400` `validation_failed`
- [ ] Implementation stays in one small package (controller + service)
- [ ] `mvn test` passes
- [ ] Thin architecture + short impl-plan exist under `docs/`
- [ ] README has a short Labels section

## Open questions

None.

## Sources

- Chat: small 0.5.3 playbook run (session auto-detect + split delivery-log)

## Suggested slug

`named-label`
