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
