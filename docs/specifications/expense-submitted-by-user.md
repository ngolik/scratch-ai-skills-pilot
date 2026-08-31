# Expense: record which user submitted the expense

## Goal

An operator creating or viewing an expense can record which existing system
user submitted it, so finance and audit can see who originated the claim
without typing a free-text name.

## In scope

- On create and on get/view of an expense, the operator can set and see the
  submitting user (identity from the existing user catalog).
- Invalid or unknown user identity is rejected with a clear client error.
- Existing expense fields and list/get behaviour stay available.

## Out of scope / non-goals

- New authentication or registration flows
- Changing how users are stored or how login works
- UI beyond the HTTP API used by operators
- Eureka / service-registry configuration
- Historical backfill of old expenses
- OpenAPI generator or major framework upgrades

## Constraints

- Stay on the existing Java / Spring stack; do **not** silently change major versions
- `layout:` `spring-layers`
- Thin architecture (omit `complexity: high`)
- Reuse existing user identity; do not duplicate a second user table

## Unchanged contracts

- Existing expense create/get (or equivalent) keep working for callers that
  omit the new field, unless the team later makes the field mandatory
- Health / actuator endpoints unchanged
- Unrelated services and routes stay as they are

## Acceptance criteria

- [ ] Create expense can include which user submitted it; that user is an
      existing system user
- [ ] Get/view expense returns the submitting user (id and/or display field
      agreed in design)
- [ ] Unknown user identity is rejected (4xx); no silent drop of the field
- [ ] Tests cover happy path + unknown-user validation and pass via project build
- [ ] Architecture + impl-plan docs exist under `docs/` for each repo that
      actually changes

## Open questions

- [MISSING — input needed] Is the submitting user mandatory on create, or optional?
- [INFERRED — please validate] Display may need a user lookup (name/email) rather
  than storing only an id

## Sources

- Local landscape: `docs/portfolio/services.yaml` in this practice hub
- Pasted product intent (this file)

## Suggested slug

`expense-submitted-by-user`

## Tracker

Not linked. Local multi-repo coordinator pilot.

## Repos
- expenses-service — owns Expense; adds optional `UserID` validation on create + a
  get-by-id endpoint that surfaces the submitting user. Stack: Go / Gin / GORM.
  **Constraint override for this repo only**: the spec's "stay on Java/Spring"
  constraint does not apply here — confirmed from `go.mod` / `api/api.go` /
  `model/expense.go`. `layout: spring-layers` is likewise not applicable to this repo.
- auth-service — source of truth for User; **consumed as-is, no code change**.
  `GET /auth/api/users/{id}` already exists and is sufficient for validate + display.
  Stays on Java 17 / Spring Boot 3.1.5 (constraint unchanged for this repo). No
  worktree, no per-repo docs — not "a repo that actually changes" for this story.

## Cross-service contracts
- owner: auth-service / consumer: expenses-service / shape: `GET /auth/api/users/{id}`
  → `UserDTO { username, email, firstName, lastName }`, 404 via
  `GlobalExceptionHandler` when id doesn't exist — [CONFIRMED, read from
  `UserController.java` + `docs/ai-context/system-overview.md`]
- owner: expenses-service / consumer: (future callers) / shape: `Expense.UserID int`
  already exists on the model (`model/expense.go:22`) but is **not currently
  validated** against auth-service and **not currently returned by any get-by-id
  route** — only `POST /expenses/rest/add` and `GET /expenses/rest/all` exist today
  (`api/api.go`). A `GET /expenses/rest/{id}` (or similar) route is new work, not a
  contract change — [CONFIRMED absence, read from `api/api.go`, `service/expense.go`]
- `UserID` on create is **optional** — callers may omit it; existing create/get
  behaviour keeps working for callers that omit the field, per "Unchanged contracts".
- echo semantics: when a `UserID` is sent and validated OK, expenses-service stores
  and echoes back the id **it received**, not an id parsed out of `UserDTO`
  (`UserDTO` does not carry an `id` field in its response body) — [CONFIRMED, read
  from `UserDTO.java`]

## Order
1. expenses-service only — add optional UserID validation (call auth-service
   `GET /api/users/{id}` when UserID is present, reject unknown with 4xx, allow
   omitted), add a get-by-id route returning the expense + submitting user info
   (echoing the stored UserID, with an optional display lookup), tests for happy
   path, omitted-field path, and unknown-user rejection.
