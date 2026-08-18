# scratch-ai-skills-pilot

Sandbox for trying client Claude skills end-to-end.

Per-feature AI delivery cost (wall time, token usage, estimated USD) is
tracked in [`docs/ai-context/delivery-log.md`](docs/ai-context/delivery-log.md).

## Health check

Run the app:

```
mvn spring-boot:run
```

The service starts on port 8080 and exposes a health check endpoint:

| Method | Path      | Response                                  |
| ------ | --------- | ------------------------------------------ |
| GET    | `/health` | `200 OK`, `Content-Type: text/plain`, body `ok` |

```
curl -i http://localhost:8080/health
```

## Greeting API

`POST /api/v1/greetings` returns a localized greeting for a given name.
`locale` is optional (default `en`); supported values are `en`, `es`, `de`.

```
curl -i http://localhost:8080/api/v1/greetings \
  -H "Content-Type: application/json" \
  -d '{"name": "Ada", "locale": "en"}'
```

```json
{
  "message": "Hello, Ada!",
  "name": "Ada",
  "locale": "en",
  "generatedAt": "2026-08-12T10:15:30.123Z"
}
```

Invalid requests (blank/too-long/invalid-character `name`, unsupported `locale`)
return `400` with:

```json
{
  "error": "validation_failed",
  "details": [
    { "field": "name", "message": "must not be blank" }
  ]
}
```

## Farewell API

`POST /api/v1/farewells` returns a localized farewell for a given name.
`locale` is optional (default `en`); supported values are `en`, `es`, `de`.
Validation and error shape are identical to the greeting endpoint above.

```
curl -i http://localhost:8080/api/v1/farewells \
  -H "Content-Type: application/json" \
  -d '{"name": "Ada", "locale": "en"}'
```

```json
{
  "message": "Goodbye, Ada!",
  "name": "Ada",
  "locale": "en",
  "generatedAt": "2026-08-13T10:15:30.123Z"
}
```

## Counter API

`POST /api/v1/counters/{name}/increments` creates the named counter at `0`
if it doesn't exist yet, then adds `1` (no request body needed).
`GET /api/v1/counters/{name}` reads the current value. `{name}` must be
1-40 lowercase letters, digits, or hyphens, must not start or end with a
hyphen, and must not contain `--`. Counters are in-memory only and reset on
restart.

```
curl -i -X POST http://localhost:8080/api/v1/counters/jobs/increments
```

```json
{
  "name": "jobs",
  "value": 1,
  "updatedAt": "2026-08-14T10:15:30.123Z"
}
```

```
curl -i http://localhost:8080/api/v1/counters/jobs
```

An invalid `{name}` returns `400` with the same `validation_failed` shape as
greetings/farewells. Reading an unknown counter returns `404`:

```json
{
  "error": "not_found",
  "details": [
    { "field": "name", "message": "counter does not exist" }
  ]
}
```

## Toggle API

`PUT /api/v1/toggles/{name}` creates the named toggle if it doesn't exist, or
replaces its `enabled` value if it does. `GET /api/v1/toggles/{name}` reads
the current value. `{name}` follows the same slug rules as counters (1-40
lowercase letters, digits, or hyphens; no leading/trailing hyphen; no `--`).
Toggles are in-memory only and reset on restart.

```
curl -i -X PUT http://localhost:8080/api/v1/toggles/dark-mode \
  -H "Content-Type: application/json" \
  -d '{"enabled": true}'
```

```json
{
  "name": "dark-mode",
  "enabled": true,
  "updatedAt": "2026-08-17T10:15:30.123Z"
}
```

```
curl -i http://localhost:8080/api/v1/toggles/dark-mode
```

An invalid `{name}` or a missing/non-boolean `enabled` returns `400` with the
same `validation_failed` shape as greetings/farewells/counters. Reading an
unknown toggle returns `404` with the same `not_found` shape as counters:

```json
{
  "error": "not_found",
  "details": [
    { "field": "name", "message": "toggle does not exist" }
  ]
}
```

## Notes API

`POST /api/v1/notes` creates a memo note; `GET /api/v1/notes/{id}` reads one
back. `text` is required, trimmed, and must be 1-200 characters after
trimming. `id` is a server-generated UUID (never client-supplied). Notes are
in-memory only and reset on restart. This feature is built with a layered
architecture (`web` / `application` / `domain` / `infrastructure`) rather
than the single-package shape used by greetings/farewells/counters.

```
curl -i -X POST http://localhost:8080/api/v1/notes \
  -H "Content-Type: application/json" \
  -d '{"text": "Ship the plugin test"}'
```

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "text": "Ship the plugin test",
  "createdAt": "2026-08-14T10:15:30.123Z"
}
```

```
curl -i http://localhost:8080/api/v1/notes/3fa85f64-5717-4562-b3fc-2c963f66afa6
```

Blank/too-long `text` or a malformed `id` return `400` with the same
`validation_failed` shape as the other endpoints. Reading an unknown id
returns `404`:

```json
{
  "error": "not_found",
  "details": [
    { "field": "id", "message": "note does not exist" }
  ]
}
```

## Status API

`PUT /api/v1/statuses/{name}` creates the named status if it doesn't exist, or
replaces its `message` value if it does. `GET /api/v1/statuses/{name}` reads
the current value. `{name}` follows the same slug rules as counters/toggles
(1-40 lowercase letters, digits, or hyphens; no leading/trailing hyphen; no
`--`). `message` is required, trimmed, and must be 1-80 characters after
trimming. Statuses are in-memory only and reset on restart. Like toggles,
this feature uses a layered architecture
(`controller` / `dto` / `service` / `entity` / `repository` / `config`).

```
curl -i -X PUT http://localhost:8080/api/v1/statuses/build-bot \
  -H "Content-Type: application/json" \
  -d '{"message": "away"}'
```

```json
{
  "name": "build-bot",
  "message": "away",
  "updatedAt": "2026-08-18T10:15:30.123Z"
}
```

```
curl -i http://localhost:8080/api/v1/statuses/build-bot
```

An invalid `{name}` or a missing/blank/too-long `message` returns `400` with
the same `validation_failed` shape as the other endpoints. Reading an unknown
status returns `404` with the same `not_found` shape as counters/toggles:

```json
{
  "error": "not_found",
  "details": [
    { "field": "name", "message": "status does not exist" }
  ]
}
```

## Label API

`PUT /api/v1/labels/{name}` creates the named label if it doesn't exist, or
replaces its `value` if it does. `GET /api/v1/labels/{name}` reads the current
value. `{name}` follows the same slug rules as counters/toggles/statuses
(1-40 lowercase letters, digits, or hyphens; no leading/trailing hyphen; no
`--`). `value` is required, trimmed, and must be 1-32 characters after
trimming. Labels are in-memory only and reset on restart. Like counters, this
feature uses a single flat package (no classic layers).

```
curl -i -X PUT http://localhost:8080/api/v1/labels/release-channel \
  -H "Content-Type: application/json" \
  -d '{"value": "beta"}'
```

```json
{
  "name": "release-channel",
  "value": "beta",
  "updatedAt": "2026-08-18T10:15:30.123Z"
}
```

```
curl -i http://localhost:8080/api/v1/labels/release-channel
```

An invalid `{name}` or a missing/blank/too-long `value` returns `400` with
the same `validation_failed` shape as the other endpoints. Reading an unknown
label returns `404` with the same `not_found` shape as counters/toggles/
statuses:

```json
{
  "error": "not_found",
  "details": [
    { "field": "name", "message": "label does not exist" }
  ]
}
```

## Request size limit

Any request with a declared `Content-Length` over 4096 bytes is rejected with
`413` before routing/validation runs, to bound how much untrusted input the
server buffers in memory. This applies service-wide, including the counter
endpoints above. Only requests that declare `Content-Length` are guarded; a
chunked request with no declared length is not capped by this filter:

```json
{
  "error": "payload_too_large",
  "details": [
    { "field": "body", "message": "must be at most 4096 bytes" }
  ]
}
```
