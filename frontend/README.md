# DocExtract UI

Angular 22 app served by the backend under `/adeon-docextract/ui/` (iframe UI for Sachbearbeiter:in,
CSR only — ADR-013). Requirements/layout: [`doc/frontend/FRONTEND-SPEC.md`](../doc/frontend/FRONTEND-SPEC.md),
[`doc/frontend/docextract_ascii_layouts.md`](../doc/frontend/docextract_ascii_layouts.md),
[`doc/frontend/docextract-color-palette.html`](../doc/frontend/docextract-color-palette.html).

## Status

Skeleton scaffolded: routing, theme, typed API clients for the backend's OpenAPI contract
(`doc/api/docextract-api-docs.json`), and the three screens from the layout spec. Business-logic
gaps are marked `TODO` in code (e.g. `process-page.ts` — `documentId`/`repositoryId` for the DMS
write aren't resolvable from any GET endpoint yet).

## Structure

```
src/app/
  core/            # API models + HttpClient/SSE clients (core/api), framework-agnostic helpers
  features/
    import/        # Layout 1 — Dokument importieren
    process/       # Layout 2/3 — Verarbeitung + Validierung (viewer, protocol/properties/similar panels, status bar)
```

## Development server

```bash
npm start            # ng serve, proxies /adeon-docextract/api to localhost:8080 (proxy.conf.json)
```

## Build

```bash
npm run build        # ng build — prod bundle in dist/docextract-ui/browser
```

`mvn package` runs this automatically (via `frontend-maven-plugin`, see root `pom.xml`) and copies
the output into `src/main/resources/static/ui`, so it ships inside the Spring Boot jar (ADR-001).

## Tests & linting

```bash
npm test             # ng test — Vitest; includes NfA-F1 (axe-core a11y) and NfA-F2 (no
                      # bypassSecurityTrust*/[innerHTML]) specs from FRONTEND-SPEC.md
npx ng lint           # ESLint (angular-eslint)
```

NfA-F3 (initial bundle ≤ 300 kB) is enforced via the `production` budget in `angular.json`.

## Conventions

- Angular 22 best practices: [`doc/frontend/angular-best-practices.md`](../doc/frontend/angular-best-practices.md)
  (signals, `input()`/`output()`/`model()`, `@Service()`, no `NgModule`).
- Prettier formats `.ts`/`.html`/`.scss` (`.prettierrc`, checked in per `doc/CONTEXT.md` §7).
- API base path/models mirror `doc/api/docextract-api-docs.json` — regenerate that spec
  (`mvn -Popenapi-spec verify`) before touching `core/models`/`core/api` for a backend contract change.
