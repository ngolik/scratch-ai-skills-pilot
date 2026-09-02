# Test cases: waiting-delivery-cost

Source: `docs/specifications/waiting-delivery-cost.md` (AC1–AC8) and `docs/qa/waiting-delivery-cost-scope.md`. No implementation/API shape confirmed yet, so steps describe the operation in business terms ("submit a create request with...") rather than a specific route — do not read these as a locked contract.

| ID | Level | Scenario | Steps | Expected | Priority | Maps to AC |
| --- | --- | --- | --- | --- | --- | --- |
| TC-01 | api | Happy path: create with all required fields | Submit create with a valid delivery id, an identified existing operator, and an amount; no remark | Record created, tied to that delivery id, operator, and amount; remark absent | P0 | AC1 |
| TC-02 | api | Amount omitted | Submit create with delivery id + operator, amount omitted | Rejected; no record created | P0 | AC2 |
| TC-03 | api | Operator omitted / anonymous | Submit create with delivery id + amount, operator omitted or unattributed | Rejected; no record created | P0 | AC3 |
| TC-04 | api | Delivery id omitted | Submit create with operator + amount, delivery id omitted | Rejected; no record created | P0 | AC4 |
| TC-05 | api | Remark omitted | Submit valid create with all required fields, remark omitted | Succeeds; record has no remark | P1 | AC5 |
| TC-06 | api | Remark present, typical length | Submit valid create with a remark well under 500 chars | Succeeds; remark stored and returned exactly as written (no trimming assumed) | P1 | AC5 |
| TC-07 | api | Remark at boundary (exactly 500 chars) | Submit valid create with a remark of exactly 500 characters | Succeeds; remark stored and returned exactly as written | P1 | AC5 |
| TC-08 | api | Remark over boundary (501 chars) | Submit valid create with a remark of 501 characters | Hard reject; no record created; remark is not silently truncated | P1 | AC6 |
| TC-09 | api | Retrieve the created record | Create a record for a delivery, then retrieve it for that same delivery | Returns delivery id, operator, amount, and remark (or its absence) exactly as stored | P0 | AC7 |
| TC-10 | manual/design-check | No list-all surface exists | Attempt to locate any "list all waiting deliveries" capability in the built feature | Confirmed absent — feature exposes single-record create/retrieve only | P2 | AC8 |
| TC-11 | api | Genuinely existing delivery id is accepted | Submit create referencing a delivery id known to be real (not just non-empty) | Accepted — positive proof beyond "any non-null id passes" | P1 | inferred risk (AC1/AC4 depth) |
| TC-12 | api | Genuinely existing operator is accepted | Submit create referencing an operator/user known to be real (not just non-empty) | Accepted — positive proof beyond "any non-null user passes" | P1 | inferred risk (AC1/AC3 depth) |
| TC-13 | **waived** | Delivery id or operator id that is well-formed but doesn't reference anything real | — | **Blocked**: AC3/AC4 only require presence, not verified existence — expected behavior (reject as unknown vs. silently accept) is undecided | P2 | inferred risk, no AC coverage |

## Coverage check
- AC1–AC8 (spec renumbered after resolving the remark-overflow question — old AC6/AC7 are now AC7/AC8): each maps to ≥1 case.
- 1 remaining explicit waiver (TC-13) instead of an invented expected result — traces to a gap already flagged in the spec/scope, not a new one.

## Change log
- TC-08 resolved from waived to a concrete case: remark >500 chars is a hard reject, not a truncation (Product decision, this conversation). Maps to spec's new AC6; TC-09/TC-10 remapped to AC7/AC8 to match the spec's renumbering.
