# Delivery log (ROI + per-feature cost)

Lightweight measurement **without Dash0**. One row per feature after G5 / merge.
**Does not block delivery.**

Per-feature detail: `docs/ai-context/features/<slug>/cost-summary.json`

## Cost basis (important)

| Cost basis | Meaning |
| --- | --- |
| **measured** | `cost_usd` reported in Claude session records |
| **estimated** | **Projected** from token counts + list prices — **not vendor billing** |
| **mixed** | Some measured, some projected |
| **none** | No session tokens found for the feature window |

Never present **estimated** as invoiced cost. Use `display_cost` from
`cost-summary.json`.

## Log

| Date | Slug | Wall time | AI cost (USD) | Cost basis | In tok | Out tok | Rework (Y/N) | Escaped (n) | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 2026-08-12 | health-endpoint | | none | none | | | | | pre-cost-tracking; merged PR #1 |
| 2026-08-12 | echo-greeting | | none | none | | | | | pre-cost-tracking; merged PR #2 |
| 2026-08-13 | echo-farewell | | none | none | | | | | pre-cost-tracking; merged PR #3 |
| 2026-08-14 | named-counter | | $4.6475 (estimated) | estimated | ~9.0M (mostly cache read) | 68020 | | | PR #4; first cost run returned `none` due to the path-encoding bug (fixed upstream since); this row reflects a corrected recompute |
| 2026-08-14 | memo-notes | | $7.5281 (estimated) | estimated | ~18.9M (mostly cache read) | 73691 | | | PR #5; path-encoding bug above is now fixed — this is the first feature with a real cost figure. Estimated from token counts + list prices, not vendor-billed |
