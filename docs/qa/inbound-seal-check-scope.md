# Test scope: inbound-seal-check

No implementation exists yet for this slug (repo mapping is explicitly deferred to `multi-repo-coordinator` per the spec's Constraints section, and whether "sealed" is a new field or reuses an existing flag is an open question), so this scope is built from the 8 AC in `docs/specifications/inbound-seal-check.md`, not from a diff. No API shape is confirmed, so rows are written as behaviors, not routes.

## In scope

| Area | Why | Priority |
| --- | --- | --- |
| Mark a delivery sealed with identified operator + delivery id (happy path) | AC1: "a sealed fact exists for that delivery id, retrievable by the warehouse" | High |
| Mark sealed with operator missing/anonymous → rejected | AC2: "anonymous or unattributed submission is rejected" | High |
| Mark sealed with delivery id missing → rejected | AC3: "cannot be created without a delivery id" | High |
| Mark sealed with note omitted → succeeds | AC4: "an omitted note does not block creation" | Medium |
| Mark sealed with note present, ≤500 chars → succeeds, note stored as written | AC4 | Medium |
| Mark sealed with note >500 chars → hard reject, not truncated | AC5 | High |
| Retrieve the sealed mark (+ note if present) for a delivery | AC6: "can be retrieved for that delivery" | High |
| Sealed fact is distinct from that same delivery's waiting fact (`waiting-delivery-cost`) and damaged fact (`damaged-inbound-writeoff`) — marking one does not clear or overwrite another | AC1 + spec's explicit "not waiting, not damaged" framing | High |
| Sealed response/record contains no money or finance-facing field | AC8: "no money amount is recorded or exposed" | Medium |
| A "list all sealed deliveries" endpoint/view does **not** exist / is not exercised | AC7: explicit non-goal — confirms scope boundary, not a feature to test | Low |
| Delivery id references a delivery that doesn't actually exist anywhere — behavior **[inferred, not in AC]** | AC3 only requires an id to be *present*, not *valid/existing* — same gap class already flagged on `waiting-delivery-cost` and originally on `expense-submitted-by-user` | Medium — **[inferred]** |
| Operator identity references a user that doesn't actually exist anywhere — behavior **[inferred, not in AC]** | AC2 only requires a user to be *present/identified*, not *verified* — same open question the spec leaves to coordinator/engineering (identity-check pattern reuse) | Medium — **[inferred]** |
| Whether sealed is stored as a new field vs. reuses an existing warehouse flag/note on `Arrival` — **[blocked]** | Open question #1 in the spec ("not thought through yet"); test design for the underlying storage/regression risk can't be finalized until this is resolved | High — **[blocked]** |

## Out of scope

- A list view of all sealed deliveries — explicit non-goal (AC7)
- Any money/finance-facing assertions beyond confirming absence (AC8) — finance-side behavior belongs to `waiting-delivery-cost` / `damaged-inbound-writeoff`'s own scopes
- Repo/service-specific test paths (e.g. exact `arrival-service` endpoint/DTO shape) — repo assignment and field-reuse decision not yet made; blast radius unknown until coordinator maps it
- Auth/login flow testing — out of scope per spec, existing user catalog is assumed to work
- Historical backfill of past inspections — explicit non-goal (inferred, out-of-scope list)

## Risks / blast radius

- Blast radius cannot be pinned to specific paths yet — no repo assignment exists. Once the coordinator assigns repos, re-cut this scope against actual impacted paths.
- **Shared-entity risk**: `arrival-service.Arrival` already carries `isWaiting`/`remark`/`isDamaged`/`damageRemark` (confirmed, `Arrival.kt`). If "sealed" lands on the same entity, any test plan must verify setting sealed does not clobber the waiting/damaged fields (and vice versa) — the same class of risk the `damaged-inbound-writeoff` sizing note anticipated ("a delivery that is both waiting and damaged doesn't lose one description to the other"). This risk is currently **unresolved** because the field-reuse-vs-new-field question (open question #1) hasn't been decided.
- **Existence vs. presence risk carried over from `waiting-delivery-cost`/`expense-submitted-by-user`**: AC2/AC3 only require identity/delivery-id to be *present*, not proven real. Any test plan should include positive evidence of a genuinely valid delivery id and operator being accepted, not just that empty/missing values are rejected.
- If the identity check ends up making a live call to `auth-service` (as `waiting-delivery-cost` does), failure-mode testing (unknown user id, auth-service unavailable) should be added once that implementation detail is resolved.

## Pass criteria

- [ ] Mark sealed with delivery id + identified operator → record created, retrievable
- [ ] Mark sealed with operator missing/anonymous → rejected, no partial record created
- [ ] Mark sealed with delivery id missing → rejected, no partial record created
- [ ] Mark sealed with note omitted → succeeds
- [ ] Mark sealed with note ≤500 chars → succeeds, note returned as written
- [ ] Mark sealed with note >500 chars → hard rejected; no record created; note not silently truncated
- [ ] Retrieve sealed record for a delivery → sealed flag (+ note) returned correctly, no money/finance field present
- [ ] Marking sealed does not alter that delivery's existing waiting or damaged fact, and vice versa
- [ ] No "list sealed deliveries" surface exists to accidentally test against
- [ ] **[inferred]** Confirm a *genuinely existing* delivery id is accepted, not just that a missing one is rejected
- [ ] **[inferred]** Confirm a *genuinely existing* operator/user is accepted, not just that a missing/anonymous one is rejected

## Evidence

- confirmed AC: `docs/specifications/inbound-seal-check.md` (Acceptance criteria section, AC1–AC8)
- confirmed decisions: same file, `## Decisions (Product, this conversation, 2026-09-02)`
- confirmed code: `arrival-service/src/main/kotlin/com/ngolik/arrival/entity/Arrival.kt` — `isWaiting`/`remark`/`isDamaged`/`damageRemark` exist, no seal field
- missing: whether sealed is a new field or reuses an existing flag (open question #1, blocks shared-entity regression design); whether identity validation checks real existence or just presence; repo/service assignment (deferred to `multi-repo-coordinator` by design, not a QA gap yet)
