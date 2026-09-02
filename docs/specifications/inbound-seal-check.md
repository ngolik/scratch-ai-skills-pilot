# Inbound delivery: mark sealed (inspection passed)

## Goal
An identified operator (existing system user), after inspecting an inbound delivery, can mark — tied to that delivery's id — that it is sealed (inspection passed), so the next shift sees that fact directly on the delivery itself instead of via chat ("ok to put away" messages). — [confirmed, `docs/specifications/_thin-inbound-seal-check.md`; identified-user/single-record shape — Product decision, 2026-09-02]

## In scope
- An identified operator (existing system user) marks a delivery as sealed (inspection passed), tied to a specific delivery id.
- An optional plain-text note, up to 500 characters.
- The sealed mark (+ note, if present) can be retrieved for that delivery — a warehouse-facing fact only.

## Out of scope
- Anonymous or unattributed submission — an identified system user is required. [confirmed, Product decision 2026-09-02]
- Marking sealed without a delivery id. [confirmed, Product decision 2026-09-02]
- A list view of all sealed deliveries — single-record create + view only. [confirmed, Product decision 2026-09-02]
- Any money amount or finance-facing data — the thin spec explicitly states finance does not look at this fact. [confirmed, `docs/specifications/_thin-inbound-seal-check.md`]
- Treating "sealed" as the same fact as `waiting-delivery-cost`'s waiting state or `damaged-inbound-writeoff`'s damage state — the thin spec explicitly says this is neither "waiting to put away" nor "damaged," but a separate warehouse fact. [confirmed, `docs/specifications/_thin-inbound-seal-check.md`]
- Whether "sealed" is stored as a new dedicated mark or reuses an existing warehouse flag/note field — not a Product-level decision; left open below. [confirmed gap, `docs/specifications/_thin-inbound-seal-check.md`: "not thought through yet"]
- New login/identity flows beyond using an existing system user. [INFERRED — please validate, pattern from sibling specs, not independently decided]
- UI beyond whatever operator interface/API is already used for deliveries. [INFERRED — please validate, pattern from sibling specs, not independently decided]
- Historical backfill of past inspections. [INFERRED — please validate, pattern from sibling specs, not independently decided]

## Acceptance criteria
1. Given an identified operator (existing system user) and a delivery id, when they mark the delivery as sealed (inspection passed), then a sealed fact exists for that delivery id, retrievable by the warehouse — distinct from any waiting or damaged fact recorded for the same delivery.
2. A sealed mark cannot be created without an identified system user — anonymous or unattributed submission is rejected.
3. A sealed mark cannot be created without a delivery id.
4. The operator may optionally include a plain-text note describing the inspection; an omitted note does not block creation.
5. A note exceeding 500 characters is rejected outright (hard reject) — not silently truncated.
6. The sealed mark (+ note, if present) can be retrieved for that delivery, so the next shift sees it on the delivery itself rather than via a side channel (chat).
7. A list of all sealed deliveries is explicitly out of scope for this story.
8. No money amount is recorded or exposed as part of this fact.

## Constraints
- [MISSING — input needed] No stack constraints stated by the source spec.
- Repository/service assignment is intentionally not made here — Product does not assign repos; deferred to `multi-repo-coordinator` after this spec is approved (same convention used by `waiting-delivery-cost` and `damaged-inbound-writeoff`).

## Dependencies
- `docs/specifications/waiting-delivery-cost.md` — structurally similar shipped precedent (delivery-tied fact, identified-operator requirement, single-record create+view, 500-char optional-note pattern). This story's sealed fact must stay distinguishable from that story's waiting fact for the same delivery — both are per-delivery facts, not assumed to be mutually exclusive.
- `docs/specifications/damaged-inbound-writeoff.md` — same relationship: sealed and damaged are explicitly separate facts per the thin spec, not to be merged.
- `docs/specifications/expense-submitted-by-user.md` — whether the identified-operator check reuses that shipped `UserID`/auth-service-validation pattern is an implementation question, not decided by Product.

## Open questions
1. ~~Whether "sealed" is a new dedicated mark or reuses an existing flag~~ — **resolved**, `multi-repo-coordinator`, 2026-09-02: new dedicated `isSealed`/`sealNote` fields, separate from `isWaiting`/`isDamaged`. See `## Repos`.
2. ~~Whether the identified-operator check reuses `expense-submitted-by-user`'s pattern~~ — **resolved**, `multi-repo-coordinator`, 2026-09-02: this story has no `expenses-service` leg, so `arrival-service` gains its own live call to `auth-service` (first for that repo) rather than reusing `expenses-service`'s `authclient` package. See `## Repos`.
3. ~~Repo/service assignment~~ — **resolved**, `multi-repo-coordinator`, 2026-09-02: `arrival-service` only; `auth-service` consumed-as-is. See `## Repos`.

## Decisions (Product, this conversation, 2026-09-02)
1. The submitting operator must be an identified, existing system user — no anonymous submission. (Matches `waiting-delivery-cost` / `damaged-inbound-writeoff` precedent.)
2. Scope is single-record create + view; a list of all sealed deliveries is out of scope.
3. Note is optional; if present, plain text, maximum 500 characters, hard reject over (not truncated).

## Evidence notes
- confirmed: goal wording + "separate fact, not waiting, not damaged, no money amount" framing (`docs/specifications/_thin-inbound-seal-check.md`); Decisions 1-3 above (Product, this conversation, 2026-09-02); `Arrival` entity has `isWaiting`/`remark`/`isDamaged`/`damageRemark` fields but no seal/inspection field today — `arrival-service/src/main/kotlin/com/ngolik/arrival/entity/Arrival.kt`.
- inferred: out-of-scope items about login/identity flows, UI, historical backfill — carried by analogy to sibling specs, not independently stated by this thin spec.
- missing: implementation approach for the sealed mark (new field vs reuse); whether identity check reuses `expense-submitted-by-user`'s pattern; repo/service assignment.

## Suggested slug
`inbound-seal-check`

## Tracker
Not linked. Local Product pack draft.

## Repos
- `arrival-service` — owns `Arrival` (sealed fact). Adds `isSealed: Boolean = false` and a dedicated `sealNote: String?` (`@Size(max=500)`, hard-reject over) — separate fields from `remark`/`damageRemark`, so a delivery that is waiting/damaged/sealed at once doesn't lose any one description to another. New `PUT /api/arrivals/{id}/sealed` endpoint mirroring `/waiting`/`/damaged`, but with a **required** body `MarkArrivalSealedRequest(operatorId: Long, note: String? = null)` — unlike its siblings, `operatorId` is mandatory (AC2/AC3), so the body can't be optional. `GET /api/arrivals/{id}` extended to return the two new fields. No change to create/get-all/waiting/damaged endpoints. Stack: Kotlin 1.8.22 / Spring Boot 3.1.5 / JPA — confirmed from `build.gradle.kts`.
- **New for this repo**: `arrival-service`'s existing `markAsWaiting`/`markAsDamaged` endpoints take no operator/identity parameter — identity is only ever checked on `expenses-service`'s side of those two sibling stories. This story has no `expenses-service` leg (no money), so satisfying AC2 requires `arrival-service`'s first-ever outbound HTTP call: a live `GET /auth/api/users/{operatorId}` against `auth-service` before accepting the seal mark. New `RestTemplate` bean + a small `authclient` package (`UserValidator` interface + `HttpUserValidator` impl), naming mirrored from `expenses-service`'s existing Go `authclient` package for cross-repo discoverability. Unknown/invalid operator → 400; auth-service unreachable/unexpected status → 502 (mirrors `expenses-service`'s `ValidationError`/`UpstreamError` split). `operatorId` is validated but **not persisted** on `Arrival` — no AC requires retrieving who sealed it (Product decision, this conversation, 2026-09-02).
- `auth-service` — consumed as-is, no code change. `GET /auth/api/users/{id}` → `UserDTO{username, email, firstName, lastName}`, same endpoint `expenses-service` already calls; `arrival-service` becomes a new consumer of it.
- `expenses-service` — OUT, no branch. Spec explicitly excludes money/finance for this story.
- `api-gateway` — OUT, no branch. No stated reason in the spec.
- `eureka` — OUT, no branch. `default_involvement: skip`.

## Cross-service contracts
- owner: `auth-service` / consumer: `arrival-service` (**new** consumer) / shape: `GET /auth/api/users/{id}` → `UserDTO{username,email,firstName,lastName}` — unchanged endpoint, but the first time `arrival-service` calls it (previously only `expenses-service` did, for `waiting-delivery-cost`/`damaged-inbound-writeoff`).
- `arrival-service.Arrival.isSealed`/`sealNote` are independent of `isWaiting`/`remark`/`isDamaged`/`damageRemark` on the same entity — no correlation needed with `expenses-service` for this story (no money leg).

## Order
Single repo does code (`arrival-service`); `auth-service` is a runtime dependency on an already-shipped endpoint, not a blocking new contract — no parallel fan-out needed for this story.
