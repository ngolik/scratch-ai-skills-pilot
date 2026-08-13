# scratch-ai-skills-pilot

Sandbox for trying client Claude skills end-to-end.

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

## Request size limit

Both `POST` endpoints reject request bodies larger than 4096 bytes with `413`
before validation runs, to bound how much untrusted input the server buffers
in memory:

```json
{
  "error": "payload_too_large",
  "details": [
    { "field": "body", "message": "must be at most 4096 bytes" }
  ]
}
```
