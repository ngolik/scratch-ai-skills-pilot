# Damaged inbound delivery: record damage and write-off amount

## Goal
An identified operator (existing system user), when an inbound delivery has damaged goods, can record — in one action, tied to that delivery's id — that it is damaged together with a write-off money amount, so warehouse (damage + optional remark) and finance (write-off amount) see one shared picture instead of photos/numbers in chat. — [confirmed, `docs/specifications/_thin-damaged-inbound-writeoff.md`; mandatory-amount/identified-user/delivery-id/single-record shape — Product decision, 2026-09-01]

## In scope
- An identified operator (existing system user) creates one record marking a delivery as damaged, together with a mandatory write-off amount, tied to a specific delivery id.
- An optional plain-text remark describing the damage, up to 500 characters.
- The record can be retrieved for that delivery (warehouse sees the damage flag + remark; finance sees the write-off amount, kept distinct from any wait-cost amount for the same delivery).

## Out of scope
- Anonymous or unattributed submission — an identified system user is required. [confirmed, Product decision 2026-09-01]
- Recording a write-off amount without a delivery id. [confirmed, Product decision 2026-09-01]
- Recording "damaged" without a write-off amount — the two are inseparable in this flow. [confirmed, Product decision 2026-09-01]
- A list view of all damaged deliveries — single-record create + view only. [confirmed, Product decision 2026-09-01]
- Automatic write-off calculation — amount is operator-entered. [INFERRED — please validate]
- Historical backfill of past damaged deliveries. [INFERRED — please validate]
- UI beyond whatever operator interface/API is already used for deliveries. [INFERRED — please validate]
- New login/identity flows beyond using an existing system user. [INFERRED — please validate]
- Merging with the shipped `waiting-delivery-cost` flow — the source thin spec explicitly says *"this is damage, not waiting"*; treated as a distinct fact/record type unless Product later decides to unify them. [confirmed, `docs/specifications/_thin-damaged-inbound-writeoff.md`]

## Acceptance criteria
1. Given an identified operator and a delivery id, when they record that the delivery is damaged together with a write-off amount, then a damage record (warehouse-facing: damage flag + optional remark) and a write-off amount (finance-facing) both exist, correlated by that same delivery id — not assumed to be one combined artefact unless a later architecture step decides otherwise.
2. A record cannot be created without a write-off amount — a missing amount is rejected, not silently accepted.
3. A record cannot be created without an identified system user — anonymous or unattributed submission is rejected.
4. A record cannot be created without a delivery id.
5. The operator may optionally include a plain-text remark describing the damage; an omitted remark does not block creation.
6. A remark exceeding 500 characters is rejected outright (hard reject) — not silently truncated.
7. The created record (damage flag + write-off amount + optional remark) can be retrieved for that delivery.
8. The write-off amount recorded here is identifiable as distinct from any wait-cost amount recorded under the separate `waiting-delivery-cost` story for the same delivery id — finance must not see the two conflated as one figure or running total.
9. A list of all damaged deliveries is explicitly out of scope for this story.

## Constraints
- [MISSING — input needed] No stack constraints stated by the source spec.
- Repository/service assignment is intentionally not made here — Product does not assign repos; deferred to `multi-repo-coordinator` after this spec is approved (same convention used by `waiting-delivery-cost`).

## Dependencies
- `docs/specifications/waiting-delivery-cost.md` — structurally similar shipped precedent (delivery-tied fact + money amount, warehouse/finance split, identified-operator + mandatory-delivery-id pattern), and now also a direct dependency via AC8 (this story's write-off amount must stay distinguishable from that story's wait-cost amount for the same delivery). Do not merge the two record types without a separate Product decision.
- `docs/specifications/expense-submitted-by-user.md` — whether the identified-operator check reuses that shipped `UserID`/auth-service-validation pattern is an implementation question, not decided by Product.
- `expenses-service.Expense` has no field for damage/write-off today. Its existing `DeliveryID`/`Amount` fields are documented in code as belonging specifically to the `waiting-delivery-cost` creation path — whether this story reuses them (with a type/category discriminator to satisfy AC8) or adds dedicated fields is an implementation/architecture question, not decided by Product. [confirmed absence, `expenses-service/model/expense.go`]
- `arrival-service.Arrival` has no damage-state field today (only `isWaiting`/`remark`). [confirmed, `arrival-service/src/main/kotlin/com/ngolik/arrival/entity/Arrival.kt`]

## Sizing signal
Small — single create + single get-by-delivery, across at most two services, no new UI; comparable in size to the shipped `waiting-delivery-cost` story (one PR per service). [INFERRED — please validate]

## Open questions
1. ~~How `expenses-service` keeps damage write-offs distinct from wait-cost amounts for AC8~~ — **resolved**, `multi-repo-coordinator`, 2026-09-01: new `Expense.IsDamageWriteOff bool` discriminator field. See `## Cross-service contracts`.
2. ~~Repo/service assignment~~ — **resolved**, `multi-repo-coordinator`, 2026-09-01: `arrival-service` + `expenses-service`, `auth-service` consumed-as-is. See `## Repos`.

## Decisions (Product, this conversation, 2026-09-01)
1. Write-off amount is mandatory together with the "damaged" fact — no amount, no record.
2. The submitting operator must be an identified, existing system user — no anonymous submission.
3. A delivery id is mandatory — no delivery-less write-off record under this flow.
4. Scope is single-record create + view; a list of all damaged deliveries is out of scope.
5. Remark is optional; if present, plain text, maximum 500 characters, hard reject over (not truncated). — refinement, 2026-09-01
6. The write-off amount must be business-distinguishable from a `waiting-delivery-cost` amount for the same delivery — added as AC8. — refinement, 2026-09-01

## Evidence notes
- confirmed: goal wording + two-facts/two-audiences framing (`docs/specifications/_thin-damaged-inbound-writeoff.md`) plus decisions 1–6 above; `Arrival` entity has no damage field, `Expense` model has no write-off field (direct repo reads, 2026-09-01).
- inferred: out-of-scope items about auto-calculation, backfill, UI, login flows; sizing signal — carried by analogy to `waiting-delivery-cost`, not independently stated by this thin spec.
- missing: implementation approach for AC8 (field reuse vs. new fields); repo/service assignment.

## Change log
- 2026-09-01 (story-refinement): Resolved remark max length (500 chars, hard reject) → new AC6, old AC5's parenthetical removed. Added AC8 (write-off amount must be distinguishable from wait-cost amount) per Product decision — business-language only, no field/endpoint shape locked. Added Sizing signal section (was a readiness gap). Renumbered AC6→7, AC7→9. Closed open questions #1 (remark length) and folded #2 (distinctness) into AC8 + a narrower open question about *how* to implement it.
- 2026-09-01 (multi-repo-coordinator): Two-domain-facts stop resolved as Option B (two writers, correlated by delivery id) — matches AC1's existing wording, no `story-refinement` round-trip needed. Added `## Repos`, `## Cross-service contracts`, `## Order`. Closed both remaining open questions: AC8's distinctness mechanism (new `Expense.IsDamageWriteOff` field) and repo assignment.

## Suggested slug
`damaged-inbound-writeoff`

## Tracker
Not linked. Local Product pack draft.

## Repos
- `arrival-service` — owns `Arrival` (damage fact). Adds `isDamaged: Boolean = false` and a dedicated `damageRemark: String?` (`@Size(max=500)`, hard-reject over) — a **separate** field from the existing `remark` (which stays the waiting-flow's remark), so a delivery that is both waiting and damaged doesn't lose one description to the other. New `PUT /api/arrivals/{id}/damaged` endpoint mirroring the existing `PUT /api/arrivals/{id}/waiting` (optional body `MarkArrivalDamagedRequest(remark)`, sets `isDamaged = true`). `GET /api/arrivals/{id}` extended to return the two new fields. No change to create/get-all/waiting endpoints. Stack: Kotlin 1.8.22 / Spring Boot 3.1.5 / JPA — confirmed from `build.gradle.kts`.
- `expenses-service` — owns money. New creation path `POST /expenses/rest/damage-writeoff` (mirrors `/waiting-cost`): requires `DeliveryID`, `UserID` (validated live against `auth-service` via the existing `authclient.HTTPUserValidator`/`service.UserValidator` seam), `Amount`; reuses the existing `Expense.DeliveryID`/`Amount` fields but sets a **new** `IsDamageWriteOff bool` field (default `false`) — the AC8 discriminator, so a damage-writeoff record and a wait-cost record sharing the same `DeliveryID`/`Amount` field *types* remain distinguishable by this flag. New `GET /expenses/rest/damage-writeoff/{id}` returns `DamageWriteOffResponse{id, deliveryId, userId, amount}`. Existing `/add`, `/all`, `/waiting-cost*` paths and the `Expense` struct's other fields are untouched. Stack: Go / Gin / GORM — confirmed from `go.mod`.
- `auth-service` — consumed as-is, no code change. `GET /auth/api/users/{id}` → `UserDTO{username, email, firstName, lastName}`, same wiring `waiting-delivery-cost` already uses.
- `api-gateway` — OUT, no branch. No stated reason in the spec.
- `eureka` — OUT, no branch. `default_involvement: skip`.

## Cross-service contracts
- owner: `auth-service` / consumer: `expenses-service` / shape: `GET /auth/api/users/{id}` → `UserDTO{username,email,firstName,lastName}` — unchanged from `waiting-delivery-cost`.
- `arrival-service.Arrival.id` ↔ `expenses-service`'s `DeliveryID` — correlation only, no live call (matches sibling; existence-validation is an open QA risk, not an AC requirement).
- `expenses-service.Expense.IsDamageWriteOff` (new field) is what satisfies AC8: it distinguishes this story's records from `waiting-delivery-cost` records at the storage level, independent of the two also having separate creation/retrieval endpoints and response DTOs.

## Order
`parallel: arrival-service, expenses-service` — no live call between them for this story, matching `waiting-delivery-cost`'s precedent. `auth-service` — no work.
