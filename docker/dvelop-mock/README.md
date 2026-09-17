# dvelop-mock

WireMock container with canned responses for `identityprovider-api`, `dvelop-dmsapp`
and `dashboard-app-api`, for local development without a real d.velop system. Opt-in
via the `mock` Compose profile:

```bash
docker compose --profile mock up -d dvelop-mock
```

## Recording new stubs

WireMock can proxy real requests to a target system and record them as stubs.
Recordings are only written to `mappings/`/`__files/` when the recording is
**stopped** — nothing is persisted while it's still running.

```bash
# start recording, proxying unmatched requests to the real system
curl -X POST http://localhost:8081/__admin/recordings/start \
  -H "Content-Type: application/json" \
  -d '{"targetBaseUrl":"https://dms.local","persist":true}'

# send real requests through the mock (http://localhost:8081/...) instead of
# directly against https://dms.local

# stop recording — this is what actually writes mappings/__files to disk
curl -X POST http://localhost:8081/__admin/recordings/stop
```

By default, response bodies are only extracted into a separate `__files/*` file
when they exceed a size threshold (`textSizeThreshold`/`binarySizeThreshold`);
smaller bodies are inlined directly into the mapping JSON's `"body"` field.
To force every response body out into `__files` instead of inlining, set both
thresholds to `0` when starting the recording:

```bash
curl -X POST http://localhost:8081/__admin/recordings/start \
  -H "Content-Type: application/json" \
  -d '{"targetBaseUrl":"https://dms.local","persist":true,"extractBodyCriteria":{"textSizeThreshold":"0","binarySizeThreshold":"0"}}'
```

Before committing recorded stubs: remove noise (`favicon.ico`, `/__admi`, duplicate
scenario states), and scrub any real PII/secrets/tokens from the captured bodies —
these fixtures are dev-only mocks that end up in the repo permanently.
