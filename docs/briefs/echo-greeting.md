# Brief: Echo greeting API

## Goal

Add a small HTTP API on top of the existing Spring Boot pilot so a client can
ask for a personalized greeting and get a structured JSON response.

Keep the existing `GET /health` behavior unchanged.

## Functional requirements

1. **`POST /api/v1/greetings`**
   - Request body (JSON):
     ```json
     { "name": "Ada", "locale": "en" }
     ```
   - `name` — required, non-blank, max 40 characters, letters/spaces/hyphen/apostrophe only
   - `locale` — optional; default `"en"`; allowed values: `en`, `es`, `de`
   - Success response `200` with JSON:
     ```json
     {
       "message": "Hello, Ada!",
       "name": "Ada",
       "locale": "en",
       "generatedAt": "<ISO-8601 UTC timestamp>"
     }
     ```
   - Message templates:
     - `en` → `Hello, {name}!`
     - `es` → `¡Hola, {name}!`
     - `de` → `Hallo, {name}!`

2. **Validation errors → `400`** with JSON problem body (no stack traces):
   ```json
   {
     "error": "validation_failed",
     "details": [
       { "field": "name", "message": "must not be blank" }
     ]
   }
   ```
   Cover at least: missing/blank `name`, too-long `name`, invalid characters,
   unsupported `locale`.

3. **Wrong HTTP method** on this path may use framework defaults (no custom
   design required).

## Non-goals

- Auth / API keys
- Persistence / DB
- Rate limiting
- i18n resource bundles beyond the three hardcoded templates above
- Changing `/health` contract
- Deployment / CI beyond what already exists
- OpenAPI/Swagger UI (document the contract in architecture + README only)

## Constraints

- Stay on the **existing** Java + Spring Boot stack already in this repo
  (do **not** silently change major Boot version; if a change is required,
  update architecture + impl-plan in the same change — Decision Drift).
- Prefer a thin controller + small service (or equivalent) over putting all
  logic in the controller.
- Unit / WebMvc tests required for:
  - happy path (`en` + default locale)
  - each locale template
  - validation failures (at least blank name + unsupported locale)

## Acceptance criteria

- [ ] `POST /api/v1/greetings` returns `200` + correct message for `en`/`es`/`de`
- [ ] Invalid requests return `400` with the error shape above
- [ ] `GET /health` still returns plain text `ok`
- [ ] Tests cover happy paths and key validation cases and pass via project build
- [ ] Architecture + impl-plan docs exist under `docs/` for this feature
- [ ] README mentions the new endpoint briefly (method, path, example)

## Suggested slug

`echo-greeting`
