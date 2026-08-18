# Brief: README pointer to delivery-log (chore)

## Goal
Engineers opening the scratch README should see where per-feature cost is recorded.

## In scope
- Add a short README paragraph pointing to `docs/ai-context/delivery-log.md`
- No API / Java / test changes

## Out of scope / non-goals
- Product API changes
- Recalculating historical cost rows

## Constraints
- Stay on the existing stack
- `layout:` n/a (docs-only)

## Unchanged contracts
- All HTTP APIs, tests, existing README sections except the new pointer

## Acceptance criteria
- [ ] README mentions `docs/ai-context/delivery-log.md`
- [ ] `mvn test` still passes
- [ ] No files under `src/`

## Suggested slug
`readme-delivery-log-pointer`
