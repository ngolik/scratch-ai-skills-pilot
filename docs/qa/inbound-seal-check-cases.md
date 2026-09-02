# Test cases: inbound-seal-check

Source: `docs/specifications/inbound-seal-check.md` (AC1–AC8) and `docs/qa/inbound-seal-check-scope.md`. No implementation/API shape confirmed yet, so steps describe the operation in business terms rather than a specific route — do not read these as a locked contract.

| ID | Level | Scenario | Steps | Expected | Priority | Maps to AC |
| --- | --- | --- | --- | --- | --- | --- |
| TC-01 | api | Happy path: mark sealed with required fields | Submit sealed-mark with a valid delivery id and an identified existing operator; no note | Sealed fact exists for that delivery id, retrievable | P0 | AC1 |
| TC-02 | api | Operator missing / anonymous | Submit sealed-mark with delivery id, operator omitted or unattributed | Rejected; no record created | P0 | AC2 |
| TC-03 | api | Delivery id missing | Submit sealed-mark with operator, delivery id omitted | Rejected; no record created | P0 | AC3 |
| TC-04 | api | Note omitted | Submit valid sealed-mark, note omitted | Succeeds; record has no note | P1 | AC4 |
| TC-05 | api | Note present, typical length | Submit valid sealed-mark with a note well under 500 chars | Succeeds; note stored and returned exactly as written | P1 | AC4 |
| TC-06 | api | Note at boundary (exactly 500 chars) | Submit valid sealed-mark with a note of exactly 500 characters | Succeeds; note stored and returned exactly as written | P1 | AC5 |
| TC-07 | api | Note over boundary (501 chars) | Submit valid sealed-mark with a note of 501 characters | Hard reject; no record created; note not silently truncated | P1 | AC5 |
| TC-08 | api | Retrieve the sealed mark | Mark a delivery sealed (with note), then retrieve it for that same delivery | Returns sealed flag + note exactly as stored | P0 | AC6 |
| TC-09 | manual/design-check | No list-all-sealed surface exists | Attempt to locate any "list all sealed deliveries" capability in the built feature | Confirmed absent — feature exposes single-record create/retrieve only | P2 | AC7 |
| TC-10 | api | Retrieved sealed record has no money/finance field | Mark a delivery sealed, retrieve it | Response contains no amount/money field of any kind | P1 | AC8 |
| TC-11 | integration | Sealing a delivery does not alter its waiting fact | On a delivery already marked waiting (`waiting-delivery-cost`), mark it sealed, then retrieve both facts | Waiting fact unchanged; sealed fact independently present | P0 | AC1 (shared-entity distinctness) |
| TC-12 | integration | Sealing a delivery does not alter its damaged fact | On a delivery already marked damaged (`damaged-inbound-writeoff`), mark it sealed, then retrieve both facts | Damaged fact unchanged; sealed fact independently present | P0 | AC1 (shared-entity distinctness) |
| TC-13 | api | Genuinely existing delivery id is accepted | Submit sealed-mark referencing a delivery id known to be real (not just non-empty) | Accepted — positive proof beyond "any non-null id passes" | P1 | inferred risk (AC1/AC3 depth) |
| TC-14 | api | Genuinely existing operator is accepted | Submit sealed-mark referencing an operator/user known to be real (not just non-empty) | Accepted — positive proof beyond "any non-null user passes" | P1 | inferred risk (AC1/AC2 depth) |
| TC-15 | **waived** | Delivery id or operator id that is well-formed but doesn't reference anything real | — | **Blocked**: AC2/AC3 only require presence, not verified existence — expected behavior (reject as unknown vs. silently accept) is undecided | P2 | inferred risk, no AC coverage |

## Coverage check
- AC1–AC8: each maps to ≥1 case.
- 1 explicit waiver (TC-15), tracing to the same presence-vs-existence gap already flagged on `waiting-delivery-cost`/`expense-submitted-by-user` — not a new gap.
- The field-reuse-vs-new-field open question (spec's open question #1) does not block TC-11/TC-12: those assert observable behavior (facts stay independent), not storage shape, so they're written as concrete cases rather than waived.
