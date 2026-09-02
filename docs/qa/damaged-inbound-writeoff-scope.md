# Test scope: damaged-inbound-writeoff

No implementation exists yet (checked `arrival-service` and `expenses-service` git history/branches — no damaged/write-off work). Scoping from the refined AC in `docs/specifications/damaged-inbound-writeoff.md`.

## In scope

| Area | Why | Priority |
| --- | --- | --- |
| Create damage + write-off, correlated by delivery id | AC1 — core happy path, two-audience correlation | High |
| Reject create with missing write-off amount | AC2 | High |
| Reject create from unattributed/anonymous submitter | AC3 | High |
| Reject create with missing delivery id | AC4 | High |
| Create with omitted remark succeeds | AC5 | Medium |
| Create with remark ≤500 chars succeeds, stored as written | AC5 | Medium |
| Reject remark >500 chars — hard reject, not truncated | AC6 | Medium |
| Retrieve record for a delivery returns damage flag + write-off amount + remark | AC7 | High |
| Write-off amount stays distinguishable from a `waiting-delivery-cost` amount on the same delivery id | AC8 — cross-story correlation; **[BLOCKED — implementation mechanism undecided]**, can only test the observable ("finance sees two separate figures, not one conflated total") once the discriminator/field approach is chosen | High |
| Confirm no list-all-damaged-deliveries endpoint exists | AC9 — negative/design-check, not a functional test | Low |

## Out of scope
- Automatic write-off calculation, historical backfill, UI beyond the operator API, new login/identity flows — per spec's Out-of-scope section.
- List of all damaged deliveries — explicitly AC9.
- Merging damage records with `waiting-delivery-cost` records into one artefact — spec explicitly keeps them separate.

## Risks / blast radius
- **[INFERRED]** Delivery-id existence is unlikely to be live-validated against `arrival-service` (sibling `waiting-delivery-cost` explicitly does correlation-only, no existence check) — risk of orphan write-off records for a typo'd/nonexistent delivery id. Worth confirming, not assuming pass.
- **[INFERRED]** If the identified-operator check reuses `waiting-delivery-cost`'s live `auth-service` call pattern, the same untested-real-validator gap applies (QA coverage for `waiting-delivery-cost` flagged this: only the fake validator is exercised, no test hits a real/stubbed `auth-service`).
- **Cross-story regression risk**: if AC8 is implemented by reusing `expenses-service.Expense.DeliveryID`/`Amount` with a new discriminator, existing `waiting-delivery-cost` create/get paths become blast radius — must re-verify AC2–AC7 of `waiting-delivery-cost` are unaffected once that field is shared.
- Pre-implementation: every row above is `not-run` by definition, not pass/fail, until code lands.

## Pass criteria
- [ ] AC1–AC7, AC9 each have at least one executed (not just written) test, matching the toolchain-execution gap flagged for `waiting-delivery-cost` (code review alone was not accepted as pass there).
- [ ] AC8's implementation mechanism is decided before test cases are written for it — currently blocked.
- [ ] Cross-story regression suite for `waiting-delivery-cost` re-run if `expenses-service` fields are shared.

## Evidence
- confirmed AC: AC1–AC9, `docs/specifications/damaged-inbound-writeoff.md` (refined 2026-09-01)
- missing: AC8 implementation mechanism (blocks concrete test design for that row); repo/service assignment (blocks knowing which services/branches to target)
