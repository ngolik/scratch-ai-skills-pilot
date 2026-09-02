# Test cases: damaged-inbound-writeoff

Drafted from `docs/specifications/damaged-inbound-writeoff.md` (AC1–AC9) and `docs/qa/damaged-inbound-writeoff-scope.md`. No implementation exists yet — these are pre-implementation cases; level/route details are **[INFERRED]** where the spec doesn't confirm an API shape.

| ID | Level | Scenario | Steps | Expected | Priority | Maps to AC |
| --- | --- | --- | --- | --- | --- | --- |
| TC-01 | api | Happy path: identified operator records damage + write-off for a delivery | 1. Authenticate as an existing system user. 2. Submit damage + write-off amount for a valid delivery id. | Damage record exists (warehouse-visible: flag + remark) and write-off amount exists (finance-visible), both correlated by that delivery id — not required to be one combined artefact. | P0 | AC1 |
| TC-02 | api | Reject create with missing write-off amount | Submit damage record for a valid delivery id + operator, omitting the amount. | Rejected outright; no record created. | P0 | AC2 |
| TC-03 | api | Reject create from unattributed/anonymous submitter | Submit damage + amount without an identified system user (or with an unknown user id). | Rejected outright; no record created. | P0 | AC3 |
| TC-04 | api | Reject create with missing delivery id | Submit damage + amount + operator, omitting the delivery id. | Rejected outright; no record created. | P0 | AC4 |
| TC-05 | api | Create with omitted remark | Submit valid damage + amount + operator + delivery id, no remark field. | Creation succeeds; remark is empty/null on retrieval. | P1 | AC5 |
| TC-06 | api | Create with remark at/under 500 chars | Submit valid create with a 500-char remark. | Creation succeeds; remark stored and returned exactly as written (no rewrite). | P1 | AC5 |
| TC-07 | api | Reject remark over 500 chars | Submit valid create with a 501-char remark. | Hard rejected (not silently truncated); no partial record created. | P1 | AC6 |
| TC-08 | api | Retrieve record for a delivery | After TC-01, fetch the record for that delivery id. | Returns damage flag, write-off amount, and remark (if set) for that delivery. | P0 | AC7 |
| TC-09 | manual/design — **[BLOCKED]** | Same delivery id has both a damage write-off (this story) and a wait-cost amount (`waiting-delivery-cost`) | Create both record types for one delivery id; inspect what finance sees. | The two amounts remain distinguishable — not summed or conflated into one figure. | P0 | AC8 — waiver: concrete steps can't be written until the `expenses-service` discriminator/field-reuse mechanism is decided (see spec's Open questions #1). This case captures the required observable only; convert to a real API case once that's resolved. |
| TC-10 | manual/design | No list-all-damaged-deliveries endpoint exists | Inspect the service's exposed routes for a list/filter-by-damaged endpoint. | No such endpoint exists. | P2 | AC9 |

All 9 confirmed AC map to ≥1 case; AC8 carries an explicit waiver rather than an invented endpoint.
