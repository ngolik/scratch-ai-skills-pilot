# Inbound delivery: record why goods were held

## Goal

A receiving operator registering an inbound delivery can attach a short
internal remark when the goods are held (not put away yet), so the next
shift and audit can see why the delivery is waiting — without a free-text
side channel (email, chat, paper).

## In scope

- On create and on view of an inbound delivery, the operator can set and
  see an optional hold remark.
- An empty or omitted remark is allowed (delivery is not held, or the
  reason is not recorded yet).
- If a remark is present, it is stored and returned as written (no silent
  drop or rewrite).
- Existing create / list / view behaviour for deliveries without a remark
  stays available.

## Out of scope / non-goals

- New login or identity flows
- Warehouse UI beyond the operator API already used for deliveries
- Automatic hold rules (capacity, quality sampling, customs)
- Historical backfill of old deliveries
- Notifications, email, or chat
- Major framework or platform upgrades

## Constraints

- Stay on the existing stack; do **not** silently change major versions
- Thin change; do not expand into a full warehouse workflow

## Unchanged contracts

- Callers that omit the remark keep working as today
- Health / readiness endpoints unchanged
- Unrelated records and routes stay as they are

## Acceptance criteria

- [ ] Create inbound delivery can include an optional hold remark
- [ ] View of that delivery returns the same remark text
- [ ] Omitted or empty remark does not block create
- [ ] Tests cover happy path + omitted remark and pass via project build
- [ ] Architecture + impl-plan docs exist under `docs/` for each area that
      actually changes

## Open questions

- [MISSING — input needed] Is there a maximum length for the remark?
- [MISSING — input needed] May the operator edit the remark after create,
  or is it write-once?
- [INFERRED — please validate] “Held” is a remark on an existing delivery
  record, not a new document type

## Sources

- Pasted product intent (this file)

## Suggested slug

`inbound-hold-remark`

## Tracker

Not linked. Local Product / QA pack smoke (no engineering PR).
