# Waiting delivery: record wait cost

## Goal
A receiving operator, when an inbound delivery is sitting waiting (not yet put away), can record — in one action, as an identified system user — that it is waiting together with the cost amount for that wait, tied to that delivery's id, so finance and the warehouse see one shared record instead of amounts being sent over chat/email. — [confirmed, `docs/specifications/_thin-waiting-delivery-cost.md`; mandatory-amount/identified-user/delivery-id shape — Product decision, this conversation, 2026-08-31]

## In scope
- An identified operator (existing system user) creates one record marking a delivery as waiting, together with a mandatory cost amount, tied to a specific delivery id.
- An optional plain-text remark, up to 500 characters.
- The operator can retrieve that single record for the delivery.

## Out of scope
- Anonymous or unattributed submission — an identified system user is required. [confirmed, Product decision 2026-08-31]
- Recording a cost without a delivery id — no delivery-less/freestanding cost under this flow. [confirmed, Product decision 2026-08-31]
- Recording "waiting" without an amount — the two are inseparable in this flow. [confirmed, Product decision 2026-08-31]
- A list view of all waiting deliveries — single-record create + view only. [confirmed, Product decision 2026-08-31]
- A separate "held + remark" story — this spec absorbs `inbound-hold-remark.md`'s intent as one unified state; that spec is not to be built separately. [confirmed, Product decision 2026-08-31]
- New login/identity flows beyond using an existing system user. [INFERRED — please validate, pattern from sibling specs, not independently decided]
- Automatic cost calculation (e.g. auto-computed demurrage rates) — amount is operator-entered. [inferred, unchanged by this pass]
- Historical backfill of past waiting deliveries. [INFERRED — please validate, pattern from sibling specs, not independently decided]
- UI beyond whatever operator interface/API is already used for deliveries. [INFERRED — please validate, pattern from sibling specs, not independently decided]

## Acceptance criteria
1. Given an identified operator (existing system user) and a delivery id, when they record that the delivery is waiting together with a cost amount, then a waiting record exists in `arrival-service` for that delivery id, and a cost record exists in `expenses-service` tied to that same delivery id, that operator, and that amount — the two are correlated by delivery id, not a single combined record. (Reworded from "one record is created" — see Change log.)
2. A record cannot be created without a cost amount — a missing amount is rejected, not silently accepted.
3. A record cannot be created without an identified system user — anonymous or unattributed submission is rejected.
4. A record cannot be created without a delivery id — there is no delivery-less cost under this flow.
5. The operator may optionally include a plain-text remark; if present, it must not exceed 500 characters. An omitted remark does not block creation.
6. A remark exceeding 500 characters is rejected outright (hard reject) — it is not silently truncated.
7. The created record (waiting state + amount + optional remark) can be retrieved as a single record for that delivery.
8. A list of all waiting deliveries is explicitly out of scope for this story.

## Constraints
- [MISSING — input needed] No stack constraints stated by the source spec.
- Repository/service assignment is intentionally not made here. Product decision (2026-08-31): repo mapping is deferred to `multi-repo-coordinator` after this spec is approved — Product does not assign repos.

## Dependencies
- `docs/specifications/inbound-hold-remark.md` — Product decision (2026-08-31): this spec now absorbs that one's intent (a single "waiting/held" state + amount, not two separate statuses). Do not implement `inbound-hold-remark.md` as a separate story.
- `docs/specifications/expense-submitted-by-user.md` — still relevant: whether the "identified operator" requirement here reuses that shipped `UserID` field/pattern on `expenses-service.Expense`, or needs its own check, is an implementation question left for the coordinator/engineering step, not decided by Product.
- `expenses-service.Expense` has no field today linking an expense to a delivery id — new work regardless of eventual repo split. [confirmed, `expenses-service/model/expense.go`]
- `arrival-service.Arrival` has no waiting/held state field today. [confirmed, `arrival-service/src/main/kotlin/com/ngolik/arrival/entity/Arrival.kt`]

## Open questions
1. [MISSING] Whether the identified-operator check reuses the existing `UserID`/user-catalog pattern from `expense-submitted-by-user`, or needs a new check — left to coordinator/engineering.
2. [MISSING] Repo/service assignment — explicitly deferred to `multi-repo-coordinator` per decision 7, not a Product-level gap.

## Decisions (Product, this conversation, 2026-08-31)
1. Amount is mandatory together with the "waiting" fact — no amount, no record.
2. The submitting operator must be an identified, existing system user — no anonymous submission.
3. A delivery id is mandatory — no delivery-less cost under this flow.
4. Scope is single-record create + view; a list of all waiting deliveries is out of scope.
5. Remark is optional; if present, plain text, maximum 500 characters.
6. This spec is one unified "waiting/held" state + amount, not two separate statuses. `inbound-hold-remark.md` is not to be built as a separate story.
7. Product does not assign repositories; repo mapping happens in `multi-repo-coordinator` after this spec is approved.
8. A remark exceeding 500 characters is a hard rejection, not a silent truncation.

## Evidence notes
- confirmed: goal wording (`docs/specifications/_thin-waiting-delivery-cost.md`) plus all 8 decisions above (Product, this conversation, 2026-08-31); `Arrival` entity has no waiting/held field — `arrival-service/src/main/kotlin/com/ngolik/arrival/entity/Arrival.kt`; `Expense` model has no delivery-link field — `expenses-service/model/expense.go`.
- inferred: out-of-scope items about login/identity flows beyond existing users, automatic cost calculation, historical backfill, UI beyond existing API — carried forward, unchanged by this pass.
- missing: whether operator identity reuses `expense-submitted-by-user`'s `UserID` pattern; repo/service assignment (deferred by design, not an oversight).

## Change log
- 2026-08-31 (story-refinement, post-implementation): AC1 reworded from "one record is created tied to that delivery id, that user, and that amount" to explicitly describe two correlated records (arrival-service + expenses-service), matching the `## Repos`/`## Cross-service contracts` architecture already recorded in this spec. Found via `docs/qa/waiting-delivery-cost-coverage.md`, which flagged AC1's old wording as a `[CONFLICT]` against the shipped implementation (PRs `ngolik/arrival-service#5`, `ngolik/expenses-service#2`). Wording-only — no new business decision, no scope change.

## Suggested slug
`waiting-delivery-cost`

## Tracker
Not linked. Local Product pack draft.

## Repos
- arrival-service — owns `Arrival`. Adds an operational waiting state (e.g. `isWaiting: Boolean`) and an optional `remark: String?` (≤500 chars, hard reject over) to the delivery record, plus a new endpoint to set them on an *existing* arrival — today `ArrivalController`/`ArrivalService` only support create / get-all / get-by-id, no update. `GET /api/arrivals/{id}` is extended to return the new fields (warehouse-facing read). Stack: Kotlin 1.8.22 / Spring Boot 3.1.5 / JPA — confirmed from `build.gradle.kts`. `Arrival.id` is client-supplied (`@Id val id: Long`, not server-generated) — this is the shared correlation key with `expenses-service`.
- expenses-service — owns money. New, additional creation path for this story only: `DeliveryID` (new field, mandatory — reject if missing, AC4) and `UserID` (existing field, made mandatory *for this path* — validated via a live call to `auth-service`, reject if missing/unknown, AC3); `Amount` mandatory (AC2, existing field). The general `POST /expenses/rest/add` is untouched — `UserID` stays optional there, preserving `expense-submitted-by-user`'s "unchanged contracts" guarantee. Needs its own get-by-id read for the retrieve AC (finance-facing). Stack: Go / Gin / GORM — confirmed from `go.mod`. Branch is cut from `main` (Product/Engineering decision, 2026-08-31) — self-contained, does **not** depend on the unmerged `feature/expense-submitted-by-user` branch (commit `a41a016`, not on `main`); implements its own live `auth-service` call for operator validation.
- auth-service — consumed as-is, no code change. `GET /auth/api/users/{id}` → `UserDTO{username, email, firstName, lastName}` is sufficient for the identified-operator check. **Correction (found during `expenses-service` implementation, 2026-08-31):** the earlier memo above claimed the direct route was `/api/users/{id}` with no `/auth` prefix, reasoning that `/auth/` in `expense-submitted-by-user.md` was gateway-only — that was wrong. `auth-service`'s own `application.yml`/`application-dev.yml` set `server.servlet.context-path: /auth/`, which Spring applies to every route the app serves, including the controller mapped at `/api/users`. Confirmed by reading both yml files directly. `expenses-service`'s client was implemented and fixed to call `/auth/api/users/{id}`.
- api-gateway — OUT, no branch. No stated reason in the spec.
- eureka — OUT, no branch. `default_involvement: skip`.

## Cross-service contracts
- owner: `auth-service` / consumer: `expenses-service` / shape: `GET /auth/api/users/{id}` → `UserDTO{username,email,firstName,lastName}` — [confirmed, `UserController.java` + `UserDTO.java` (controller mapping) and `application.yml`/`application-dev.yml` (`/auth/` context-path), auth-service `main`]. 404-on-unknown-id behavior implemented as designed; not yet verified against a running `auth-service` instance (no live integration test in this pass).
- `arrival-service.Arrival.id` ↔ `expenses-service`'s new `DeliveryID` — **correlation only, no live call.** `expenses-service` stores the id but does not call `arrival-service` to confirm it's real — matches Decision 3's literal wording (presence, not existence-validation; existence-validation is an open QA risk, not an AC requirement).

## Order
`arrival-service` and `expenses-service` proceed **in parallel** — correlated only by sharing the same delivery id value, no blocking dependency between them (no live call from one to the other for this story). `auth-service` — no work.
