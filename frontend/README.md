# DocExtract UI

Angular app served by the backend under `/adeon-docextract/ui/` (iframe UI for Sachbearbeiter:in).

## Scaffold

Run from the repo root, using the exact project name expected by `pom.xml` (`frontend.project.name`):

```bash
npx @angular/cli new docextract-ui --directory frontend --routing --style scss
```

Ensure `frontend/angular.json` builds to `dist/docextract-ui` (Angular's default builder output already
nests the browser bundle under `dist/docextract-ui/browser`, which is what the Maven build copies).

Set the app's base href / router base to `/adeon-docextract/ui/` (e.g. `<base href="/adeon-docextract/ui/">`
in `src/index.html`) so asset and route URLs resolve correctly behind the reverse proxy.

## Build integration

Once `frontend/package.json` exists, `mvn package` automatically:
1. Installs Node/npm and runs `npm ci && npm run build` in `frontend/`.
2. Copies `frontend/dist/docextract-ui/browser/**` into `src/main/resources/static/ui`, so the Angular
   build ships inside the single Spring Boot jar (ADR-001: no separate frontend deployment).
