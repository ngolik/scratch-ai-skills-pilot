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
