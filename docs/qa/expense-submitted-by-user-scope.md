# Test scope: expense-submitted-by-user

Scope is based on the impacted paths in expenses-service PR #1 (`feature/expense-submitted-by-user`, commit `a41a016`): `api/expense.go`, `service/expense.go`, `model/expense.go`, `database/config.go`. Not a full-repo regression.

## In scope

| Area | Why | Priority |
| --- | --- | --- |
| `POST /expenses/rest/add` with valid, existing `UserID` | AC: "Create expense can include which user submitted it; that user is an existing system user" | High |
| `POST /expenses/rest/add` with `UserID` omitted | AC: "Existing expense create/get… keep working for callers that omit the new field" | High |
| `POST /expenses/rest/add` with unknown/non-existent `UserID` | AC: "Unknown user identity is rejected (4xx); no silent drop of the field" | High |
| `GET /expenses/rest/:id` (new endpoint) returns the stored `UserID` verbatim | AC: "Get/view expense returns the submitting user"; confirmed echo semantics (stores/returns the id it received, not one parsed from `UserDTO`) | High |
| `GET /expenses/rest/:id` for a non-existent expense id | Not explicitly in AC but standard for a new get-by-id route; risk of unhandled 500 vs proper 404 | Medium — **[inferred]** |
| `GET /expenses/rest/all` still works with mixed rows (some with `UserID`, some without) | AC: "Existing expense fields and list/get behaviour stay available" | Medium |
| **Validation-source risk**: does `UserExists` correctly reflect real users, or does it silently reject everyone because `model.User` is unpopulated? | **[CONFLICT]** flagged in readiness/dependency-questioning — implementation validates against a local `model.User` table, not a live auth-service call, and that table's population is unconfirmed. If empty, every `UserID` looks "unknown," which technically passes AC wording but fails the intent. | **Critical — blocks sign-off** |
| `database.MigrateDatabase` actually auto-migrates `model.User` (not just `model.Expense`) | Needed for the local-table validation path to work at all; `database/config.go` was touched by this PR | High |

## Out of scope

- auth-service changes — consumed as-is, no code change in this story
- UI — HTTP API only
- Eureka / service-registry behavior
- Historical backfill of pre-existing expenses without `UserID`
- Framework/major-version upgrades

## Risks / blast radius

- Blast radius is contained to expenses-service's `expense` create/get path; `GET /expenses/rest/all` and unrelated routes should be spot-checked but are low-risk (additive change).
- **Primary risk is not code-path breakage but semantic correctness**: unknown-user rejection could be trivially "correct" (always rejects) if the local user mirror is empty — this would pass a shallow happy/unhappy-path test suite while completely failing real-world validation. Any test plan here should include **positive evidence that a genuinely valid `UserID` is accepted**, seeded explicitly into the local table, not just that an arbitrary id is rejected.
- `go build`/`go test` were not confirmed to have run in the authoring environment per the commit message — verify CI actually executed and passed before trusting the existing `api/expense_test.go` / `service/expense_test.go` as sufficient evidence.

## Pass criteria

- [ ] Create with a seeded, existing `UserID` → 200/201, `UserID` persisted
- [ ] Create with omitted `UserID` → existing behavior unchanged, no regression
- [ ] Create with unknown `UserID` (confirmed absent from whatever store is used) → 4xx, no partial expense created
- [ ] Get-by-id echoes the exact `UserID` sent on create (not a value derived from `UserDTO`)
- [ ] Get-by-id on a non-existent expense id → sensible 4xx (not a raw 500)
- [ ] `GET /expenses/rest/all` unaffected by the new field/route
- [ ] **Confirm (not assume) that at least one real, valid `UserID` is accepted** — closes the "empty table = always reject" risk
- [ ] CI shows `go build ./... && go test ./...` green on PR #1

## Evidence

- confirmed AC: `docs/specifications/expense-submitted-by-user.md` (Acceptance criteria section); echo semantics and route absence confirmed from `api/api.go`, `service/expense.go`, `model/expense.go` on `main` and commit `a41a016`
- **[CONFLICT — unresolved]**: validation-source ambiguity (local table vs auth-service call) carried over from `/demo-company-core:readiness` and `/demo-product:dependency-questioning` — the "Critical" row above exists because of this, not from AC text alone
- missing: whether `model.User` is seeded/synced at all (no evidence found in the diff)
- **Update**: CI check `build` on PR #1 confirmed **pass** (`gh pr checks 1`, run 32849537183) — the "CI status unconfirmed" gap below is now resolved for the build/test-run item specifically; see Acceptance coverage for what remains open.

## Acceptance coverage

| AC | Covered by | Result | Gap |
| --- | --- | --- | --- |
| AC1 — Create expense can include which user submitted it; that user is an existing system user | `TestAddExpense_ValidUserID` (service, fake repo, id=42) | pass (as-tested) | No HTTP-level "valid known user accepted" test — `api/expense_test.go` only covers unknown-user and omitted-user POST cases. Both service- and api-level tests stub `UserExists` via an in-memory fake — **the real `gormRepository.UserExists` (querying the actual `model.User` GORM table) is never exercised by any test.** Combined with the open `[CONFLICT]` on validation source, this AC has no evidence it works against real data. |
| AC2 — Get/view expense returns the submitting user (id and/or display field agreed in design) | `TestGetExpenseById_EchoesStoredUserID` (service), `TestGetExpenseByIdHandler_EchoesUserID_Returns200` (api) | pass — id only | "Display field" half of the AC was never resolved ("agreed in design" left open per story-refinement) and isn't implemented or tested. This AC should be split; the id-echo half is covered, the display-field half is not built. |
| AC3 — Unknown user identity is rejected (4xx); no silent drop of the field | `TestAddExpense_UnknownUserID` (service), `TestAddExpenseHandler_UnknownUser_Returns400` (api) | pass (as-tested) | Same fake-repo caveat as AC1: proves the logic rejects when `UserExists` returns false, not that a real database lookup correctly distinguishes "genuinely unknown" from "table is empty so everything is unknown." |
| AC4 — Tests cover happy path + unknown-user validation and pass via project build | 9 named test functions across `api/expense_test.go` + `service/expense_test.go`; CI check `build` = pass on PR #1 | pass | CI confirmed green, resolving the earlier build/test gap. Residual: "happy path" as tested is fake-repo-only (see AC1). |
| AC5 — Architecture + impl-plan docs exist under `docs/` for each repo that actually changes | `docs/architecture/expense-submitted-by-user.md`, `docs/impl-plans/expense-submitted-by-user.md` present in commit `a41a016` | pass | none |
| get-by-id 404 for non-existent expense (implied, not explicit AC) | `TestGetExpenseById_NotFound` (service), `TestGetExpenseByIdHandler_NotFound_Returns404` (api) | pass | none — bonus coverage beyond stated AC |
| `GET /expenses/rest/all` unaffected | none | not-run | No test in this diff touches `GetExpensesHandler`/`GetAllExpenses`. |

### Coverage summary

- Covered: 3 / 5 stated AC fully (AC3, AC4, AC5); 2 / 5 partially (AC1, AC2)
- Gaps:
  1. No test proves the real `gormRepository.UserExists` (actual DB query against `model.User`) works — every test uses an in-memory fake. Given the unresolved validation-source conflict, the suite would look identical whether the real local-table path works correctly or is completely broken/empty.
  2. No HTTP-level happy-path test for "valid known `UserID` accepted" (only service-level).
  3. AC2's display-field half is unbuilt and untested (open design question, not a regression).
  4. `GET /expenses/rest/all` regression not explicitly covered by this diff.
- Verdict: **gaps-remain**
