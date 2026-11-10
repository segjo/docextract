# llm-mock

WireMock container with canned OpenAI-compatible responses for the chat-completions
(`POST /v1/chat/completions`) and embeddings (`POST /v1/embeddings`) endpoints, standing in for
the real LLM/embedding provider (ADR-009/-011) for local development and CI smoke tests without
needing real provider credentials or a locally pulled Ollama model ("LLM als Mock", see
doc/CONTEXT.md C-6 / doc/ARCHITECTURE.md § "ci"-Profil). Opt-in via the `mock` Compose profile:

```bash
docker compose --profile mock up -d llm-mock
```

Point the app at it (either via `application.yml` or env overrides):

```yaml
docextract:
  adapters:
    llm:
      base-url: http://localhost:${LLM_MOCK_PORT:-8082}
      api-key:
    embedding:
      base-url: http://localhost:${LLM_MOCK_PORT:-8082}/v1
      api-key:
```

## What this mock is (and isn't)

- `chat-completions.json` always returns the same canned `message.content` regardless of the
  request's `messages`/`response_format` JSON schema. It's only good for proving the chat-call
  wiring works end to end (base URL, auth header, request/response shape) — not for exercising
  `ExtractAttributesService`'s schema validation (`T-1`) with realistic content. Real functional
  tests use the in-process `OpenAiCompatibleStub` (test sources, `shared.testsupport`), whose chat
  answer depends on the request (see `IngestToExtractionResultHttpTest`).
- `embeddings.json` always returns the same single canned 1024-dimension vector (matching the
  `qwen3-embedding:0.6b` dimension the `embedding` table's `vector(1024)` column expects, see
  `V3__embedding.sql`), which is fine since the app only ever embeds one text per call
  (`StagePendingEmbeddingsService`). A request embedding more than one text at once would fail
  the adapter's `results.size() != texts.size()` check against this mock.

## Recording new stubs

WireMock can proxy real requests to a target system and record them as stubs — useful to capture
a real provider's exact response shape instead of hand-writing one. Recordings are only written to
`mappings/`/`__files/` when the recording is **stopped**.

`targetBaseUrl` is resolved from *inside the `llm-mock` container*, not from your host shell —
`localhost`/`127.0.0.1` there means the container itself, not the Ollama/other container you're
trying to proxy to. Use the other container's Compose **service name** (both must be on the
`docextract` network, which `llm-mock` already joins):

```bash
# embeddings, against the real ollama container (its in-container port 11434, not the
# host-published OLLAMA_PORT)
curl -X POST http://localhost:8082/__admin/recordings/start \
  -H "Content-Type: application/json" \
  -d '{"targetBaseUrl":"http://ollama:11434","persist":true,"extractBodyCriteria":{"textSizeThreshold":"0","binarySizeThreshold":"0"}}'

# send real requests through the mock (http://localhost:8082/...) instead of
# directly against ollama

curl -X POST http://localhost:8082/__admin/recordings/stop
```

For a provider outside Compose (e.g. the real Azure/Foundry chat endpoint), use its real
`https://` URL as `targetBaseUrl` instead — that one *is* reachable directly from inside the
container since it's a public hostname, not a container-local address.

If `targetBaseUrl` is wrong or unreachable, WireMock still returns a response for the proxied
request (an HTTP 500 describing the connection failure) and — since recording was active — that
failure response itself gets recorded and persisted as a stub with an exact-body match on stop. Watch
for `"status" : 500` in the `recordings/stop` response/mapping file; delete it from `mappings/`/
`__files/` and retry with a reachable `targetBaseUrl` rather than committing it.

Before committing recorded stubs: scrub any real API keys/tokens/PII from the captured request and
response bodies — these fixtures are dev-only mocks that end up in the repo permanently.

## Troubleshooting: "500 ... Not found in blob store" / canned stubs stop responding

Recording (start → traffic → stop) can silently shadow or replace the hand-written
`chat-completions.json`/`embeddings.json` stubs with new exact-body-match recordings. If the
app's actual request body doesn't match whatever the last recording captured (it won't, since real
requests vary — tool content, `encoding_format`, retries, etc.), it falls through to whatever else
got persisted, which has occasionally ended up referencing a response body file that was never
written to `__files/`, producing a generic Jetty `500 ... NotFoundException: Not found in blob
store: <file>.json` instead of JSON. Symptom: `curl` with one exact request body still works (it
matches a leftover exact-body stub) while the real application fails.

Fix: inspect `mappings/`/`__files/` for unexpected recorded files (anything other than
`chat-completions.json`, `embeddings.json`, `.gitkeep`) and remove them, then force WireMock to
reload from a clean container state (its stub list is otherwise kept in memory, so just editing
files on disk doesn't retroactively fix an already-running, already-corrupted container):

```bash
docker compose --profile mock down llm-mock
docker compose --profile mock up -d --build llm-mock
```

