# Test scope: waiting-delivery-cost

No implementation exists yet for this slug (repo mapping is explicitly deferred to `multi-repo-coordinator` — see spec's Decision 7), so this scope is built from the 7 confirmed AC in `docs/specifications/waiting-delivery-cost.md`, not from a diff. No API shape is confirmed, so rows are written as behaviors, not routes.

## In scope

| Area | Why | Priority |
| --- | --- | --- |
| Create a record with delivery id + identified operator + amount (happy path) | AC1: "one record is created tied to that delivery id, that user, and that amount" | High |
| Create with amount omitted → rejected | AC2: "missing amount is rejected, not silently accepted" | High |
| Create with operator/user omitted or anonymous → rejected | AC3: "anonymous or unattributed submission is rejected" | High |
| Create with delivery id omitted → rejected | AC4: "there is no delivery-less cost under this flow" | High |
| Create with remark omitted → succeeds | AC5: "omitted remark does not block creation" | Medium |
| Create with remark present, ≤500 chars → succeeds, remark stored as written | AC5 | Medium |
| Retrieve the single created record for a given delivery | AC6: "retrieved as a single record for that delivery" | High |
| A "list all waiting deliveries" endpoint/view does **not** exist / is not exercised | AC7: explicit non-goal — confirms scope boundary, not a feature to test | Low |
| Create with remark >500 chars → hard reject | AC6 (resolved): reject, not truncate — Product decision, this conversation | High |
| Delivery id references a delivery that doesn't actually exist anywhere — behavior **[inferred, not in AC]** | AC4 only requires an id to be *present*, not that it be *valid/existing*. Same class of gap the team already hit on `expense-submitted-by-user` (UserID validated only against a possibly-empty local mirror, not confirmed against real data) — worth flagging before this repeats | Medium — **[inferred]** |
| Operator identity references a user that doesn't actually exist anywhere — behavior **[inferred, not in AC]** | AC3 only requires a user to be *present/identified*, not that the identity be *verified* against a real user store — same open question the spec itself leaves to coordinator/engineering (reuse of `UserID` pattern) | Medium — **[inferred]** |

## Out of scope

- A list view of all waiting deliveries — explicit non-goal (AC7)
- Anything on `inbound-hold-remark.md` as a separate feature — spec's Decision 6 absorbs it into this one; no separate test target
- Repo/service-specific test paths (e.g. `arrival-service` vs `expenses-service` internals) — repo assignment not yet made; blast radius unknown until coordinator maps it
- Auth/login flow testing — out of scope per spec, existing user catalog is assumed to work

## Risks / blast radius

- Blast radius cannot be pinned to specific paths yet — no repo assignment exists. Once the coordinator assigns repos, this scope should be re-cut against actual impacted paths (per rule: prefer impacted paths over blanket regression).
- **Primary risk carried over from the sibling `expense-submitted-by-user` story**: "presence" validation (id/user is non-null) is trivial to pass without actually proving the reference is *real*. That story already surfaced this exact gap (validation against a possibly-unpopulated local table vs. a live source-of-truth call). Any test plan for this story should include **positive evidence of a genuinely valid delivery id and a genuinely valid operator being accepted**, not just that empty/missing values are rejected — otherwise a hollow "always accept anything non-null" implementation would pass.
- The 500-character remark boundary is now resolved: hard reject over the limit, not truncation (Product decision, this conversation) — see `docs/qa/waiting-delivery-cost-cases.md` TC-08.

## Pass criteria

- [ ] Create with delivery id + identified operator + amount → record created, all three fields persisted
- [ ] Create with amount missing → rejected, no partial record created
- [ ] Create with operator missing/anonymous → rejected, no partial record created
- [ ] Create with delivery id missing → rejected, no partial record created
- [ ] Create with remark omitted → succeeds
- [ ] Create with remark ≤500 chars → succeeds, remark returned as written
- [ ] Retrieve the single record for a delivery → all fields (waiting state, amount, user, optional remark) returned correctly
- [ ] Create with remark >500 chars → hard rejected; no record created; remark not silently truncated
- [ ] **[inferred]** Confirm a *genuinely existing* delivery id is accepted, not just that a missing one is rejected
- [ ] **[inferred]** Confirm a *genuinely existing* operator/user is accepted, not just that a missing/anonymous one is rejected
- [ ] No "list waiting deliveries" surface exists to accidentally test against

## Evidence

- confirmed AC: `docs/specifications/waiting-delivery-cost.md` (Acceptance criteria section, AC1–AC7)
- confirmed decisions: same file, `## Decisions (Product, this conversation, 2026-08-31)`
- resolved since first draft: remark-overflow enforcement is a hard reject (Product decision, this conversation) — see spec AC6
- missing: whether delivery-id/operator validation checks real existence or just presence (not addressed by any AC); repo/service assignment (deferred to `multi-repo-coordinator` by design, not a QA gap yet)
