# Manual end-to-end requests

Runnable HTTP requests for exercising the running backend against real
collaborators (d.velop mock, Gotenberg) instead of unit-test doubles. Open the
`.http` files with the VS Code [REST Client](https://marketplace.visualstudio.com/items?itemName=humao.rest-client)
extension (or the IntelliJ HTTP Client, which understands the same format).

## Prerequisites

1. Start the collaborators the ingest module talks to:
   ```bash
   docker compose --profile mock up -d dvelop-mock preview
   ```
   `dvelop-mock` stubs the identityprovider and DMS-Chunk-Store APIs;
   `preview` is the real Gotenberg/LibreOffice container.
2. Run the app so it falls back to the mock's system base URI
   (`docextract.dvelop.system-base-uri`, default `http://localhost:8081`,
   since the configured jstore at port 6380 is unreachable in this setup):
   ```bash
   mvn spring-boot:run
   ```
3. Open [ingest.http](ingest.http) and run the requests top to bottom (the
   `# @name` requests let later requests reference their JSON response body,
   e.g. `{{uploadPdf.response.body.$.previewLocation}}`).

## Files

- [ingest.http](ingest.http) — upload a native PDF and a non-PDF (rendered via
  Gotenberg), then stream both previews; plus the negative cases (missing
  credential, oversized upload).
- [fixtures/](fixtures) — small sample files used as multipart upload bodies.

## Notes

- Any `Authorization: Bearer <token>` or `AuthSessionId` cookie is accepted;
  `dvelop-mock`'s `identityprovider-validate.json` does not check the value.
- The mocked `/dms/r/{repositoryId}/blob/chunk` upload always returns the same
  static `Location`; the matching GET stub (`dms-blob-chunk-content.json`)
  serves a fixture PDF for the preview requests.
