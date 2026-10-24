# Anforderungen

Erstelle ein Frontend für das Docextract Backend.

Anforderungen an das Frontend:

- Benutzerfreundliche Oberfläche gemäss der Layout Vorgabe und der Farbpallette [`doc/frontend/docextract-color-palette.html`](/doc/frontend/docextract-color-palette.html)
- Interaktion mit dem Docextract Backend [`doc/api/docextract-api-docs.json`](/doc/api/docextract-api-docs.json)
- Client Side Rendering (CSR) für eine reaktive Benutzeroberfläche — kein SSR (Entscheid siehe
  ADR-013 in [`ARCHITECTURE.md`](/doc/ARCHITECTURE.md))
- Angular 22 with best practices instructions [`doc/frontend/angular-best-practices.md`](/doc/frontend/angular-best-practices.md)

# Bildschirme

Layout [`doc/frontend/docextract_ascii_layouts.md`](/doc/frontend/docextract_ascii_layouts.md)

# Ablauf

Der Ablauf gliedert sich in drei Bildschirme (siehe
[`docextract_ascii_layouts.md`](/doc/frontend/docextract_ascii_layouts.md)), die nacheinander
durchlaufen werden. Es gibt **keinen automatischen DMS-Write**: der Prozess endet erst nach
expliziter menschlicher Freigabe (C-2).

1. **Dokument importieren** (Layout 1)
   - Benutzer wählt ein Dokument per Drag & Drop oder Dateiauswahl (`PDF, DOCX, XLSX, TIFF, PNG,
JPG`, max. 50 MB — clientseitige Vorprüfung von Typ/Grösse als UX-Hinweis, massgeblich bleiben
     die serverseitigen Limits, FR-1/T-4).
   - Klick auf `Importieren` löst den Upload per REST aus; die API liefert `202` + `processId`
     zurück. `Abbrechen` schliesst den Dialog ohne Upload.

2. **Verarbeitung / Vorschau ausstehend** (Layout 2)
   - Nach dem Upload navigiert das Frontend zur Prozessansicht und abonniert den Fortschritt des
     `processId` per **SSE** (ADR-004).
   - Das Protokoll-Panel (rechts) zeigt die Pipelinestufen als Liste mit Status
     `laeuft … / wartet / fertig`:
     1. Upload & Eingangsprüfung (FR-1)
     2. Vorschau generieren (FR-1, Gotenberg nur für Nicht-PDF)
     3. Dokument strukturieren (FR-2, Textextraktion)
     4. Ähnliche Dokumente suchen (FR-3, Embedding + Retrieval mit Mandanten-Pre-Filter)
     5. Eigenschaften extrahieren (FR-4, LLM-Attributvorschläge)
   - Der Viewer (links) zeigt währenddessen `Vorschau wird erstellt`, bis die PDF-Vorschau verfügbar
     ist. Tritt in einer Stufe ein Fehler auf (z. B. Limit überschritten, LLM-Fehler), wird dies im
     Protokoll als Fehlerstatus angezeigt; der Ablauf bricht kontrolliert ab (kein automatischer
     Retry, kein DMS-Write).

3. **Dokumentvorschau / Eigenschaftenvalidierung** (Layout 3)
   - Sobald alle Pipelinestufen abgeschlossen sind, wechselt das rechte Panel automatisch auf den
     Tab `EIGENSCHAFTEN`: KI-Vorschläge je Feld mit Konfidenz (`%`) und Verweis auf die Quellstelle
     (`Quelle - Seite N anzeigen`, synchronisiert mit dem PDF-Viewer links). Als `kritisch`
     markierte Felder (niedrige Konfidenz) sind visuell hervorgehoben und erfordern besondere
     Aufmerksamkeit bei der Prüfung.
   - Tab `AEHNLICHE` listet die in Stufe 4 gefundenen ähnlichen Dokumente (Retrieval-Ergebnis,
     FR-3) als Entscheidungshilfe für die Validierung.
   - Tab `PROTOKOLL` bleibt als Verlauf der durchlaufenen Pipelinestufen einsehbar (Audit-Trail,
     C-4).
   - Benutzer prüft/korrigiert die vorgeschlagenen Werte (Freitext- und Auswahlfelder) und
     entscheidet:
     - `Ablehnen`: Prozess wird verworfen, `PENDING`-Embeddings und temporäre Blobs werden gelöscht
       (C-7, ADR-008), kein DMS-Write.
     - `Bestätigen & ins DMS schreiben`: löst den Consent-Gate aus (FR-5/C-2); erst danach erfolgt
       der einzige Schreibpfad ins DMS (`DmsWriteAdapter`) sowie die atomare Promotion der
       zugehörigen Embeddings von `PENDING` auf `APPROVED` (C-7).
   - Die Statuszeile am unteren Rand (`N Eigenschaften · M kritisch · K zu prüfen`) fasst den
     Prüfstand zusammen und steuert, ob `Bestätigen` aktivierbar ist.

Der Agent-Pfad (MCP, FR-6) durchläuft dieselben Stufen 1–5 ohne eigenes UI; der Consent vor dem
DMS-Write bleibt in beiden Pfaden identisch verbindlich (Adapter-Symmetrie, siehe
[`CONTEXT.md`](/doc/CONTEXT.md)).

# Tests

## Qualitätskriterien (messbar, NfA)

Je ein messbares Kriterium für Barrierefreiheit, Frontend-Sicherheit und Performance, inkl.
Prüfmethode und Testnachweis per gezielter Fehlereinspeisung/Mutation. **Status:** `frontend/` ist
gescaffoldet (siehe [`frontend/README.md`](/frontend/README.md)); NfA-F1/F2 laufen als echte Specs
in `ng test`, NfA-F3 als `production`-Budget in `angular.json`. Testrunner ist Angular 22s Default
`@angular/build:unit-test` (**Vitest**) — die Specs unten nutzen entsprechend `vitest-axe`s
`axe()`-Runner + eine einfache `expect(...).toEqual([])`-Assertion, gleiches Prüfprinzip wie unten
beschrieben.

### NfA-F1 Barrierefreiheit — WCAG 2.1 AA via axe-core

- **Kriterium (messbar):** 0 axe-core-Verstösse mit Impact `critical` oder `serious` pro Screen
  (Upload, Vorschau, Validierung — siehe `docextract_ascii_layouts.md`).
- **Prüfmethode:** automatisierter Accessibility-Test pro Standalone-Component via `axe-core` +
  `vitest-axe` im Vitest-basierten Testrunner (`ng test`), z. B.:

  ```ts
  // import.a11y.spec.ts
  import { TestBed } from "@angular/core/testing";
  import { provideRouter } from "@angular/router";
  import { provideHttpClient } from "@angular/common/http";
  import { axe } from "vitest-axe";
  import { Import } from "./import";

  describe("Import a11y", () => {
    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [Import],
        providers: [provideRouter([]), provideHttpClient()],
      }).compileComponents();
    });

    it("hat keine axe-Verstösse mit Impact critical/serious", async () => {
      const fixture = TestBed.createComponent(Import);
      fixture.detectChanges();
      const results = await axe(fixture.nativeElement as HTMLElement, {
        resultTypes: ["violations"],
      });
      const relevant = results.violations.filter((v) =>
        ["critical", "serious"].includes(v.impact ?? ""),
      );
      expect(relevant).toEqual([]);
    });
  });
  ```

- **Fehlereinspeisung (Rot-Nachweis):** Mutation entfernt das `<label>`/`aria-label` des
  Datei-Upload-`<input type="file">` (z. B. versehentlich beim Refactoring). Erwartetes Ergebnis:
  axe meldet die Regel `label` ("Form elements must have labels", Impact `critical`) → Test
  schlägt fehl, bis das Label wiederhergestellt ist.

### NfA-F2 Frontend-Sicherheit — keine Sanitizer-Bypässe (XSS)

- **Kriterium (messbar):** 0 Vorkommen von `DomSanitizer.bypassSecurityTrust*` und
  `[innerHTML]`-Bindings im gesamten `frontend/src`-Baum (Angular escaped Interpolation per
  Default; jede Umgehung ist ein potenzielles XSS — T-1-relevant, da Dokumentinhalte/LLM-Exzerpte
  als _untrusted data_ gelten).
- **Prüfmethode:** statischer Dateisystem-Test, der im selben Testlauf wie die Unit-Tests
  (`ng test`) mitläuft:

  ```ts
  // security/no-sanitizer-bypass.spec.ts
  import { readFileSync } from "node:fs";
  import { glob } from "glob";

  it("verwendet nirgends bypassSecurityTrust* oder [innerHTML]", () => {
    const offenders = glob
      .sync("src/**/*.{ts,html}")
      .filter((f) =>
        /bypassSecurityTrust|\[innerHTML\]/.test(readFileSync(f, "utf8")),
      );
    expect(offenders).toEqual([]);
  });
  ```

- **Fehlereinspeisung (Rot-Nachweis):** gezielte Mutation in der Validierungs-Komponente, die den
  LLM-Vorschlag (`sourceExcerpt`, untrusted Dokumentinhalt) ungefiltert rendert, z. B.
  `<div [innerHTML]="suggestion.sourceExcerpt"></div>` statt Text-Interpolation
  `{{ suggestion.sourceExcerpt }}`. Erwartetes Ergebnis: `offenders` enthält die Datei →
  `expect(offenders).toEqual([])` schlägt fehl (rot), bis wieder auf Interpolation umgestellt wird.

### NfA-F3 Performance — Initial-Bundle-Budget

- **Kriterium (messbar):** initiales JS-Bundle (`main` + `polyfills`, gzip) ≤ 300 KB — eigenständiges
  Frontend-Performance-Ziel, ergänzend zu NfA-2 (p95 Upload→Vorschlag ≤ 30 s Backend-seitig).
- **Prüfmethode:** Angular-CLI-Build-Budget in `angular.json` (`budgets`), hart durchgesetzt durch
  `ng build --configuration production` (Exit-Code ≠ 0 bei Überschreitung, kein zusätzliches
  Lighthouse-Tooling nötig):

  ```jsonc
  // angular.json (production configuration)
  "budgets": [
    { "type": "initial", "maximumWarning": "250kb", "maximumError": "300kb" }
  ]
  ```

- **Fehlereinspeisung (Rot-Nachweis):** Mutation fügt dem initialen Bundle eine grosse, nicht lazy
  geladene Abhängigkeit hinzu (z. B. `import * as _ from 'lodash'` statt gezieltem Sub-Import).
  Erwartetes Ergebnis: `ng build --configuration production` bricht mit
  `Error: bundle initial exceeded maximum budget` ab → Build (= Test) wird rot, bis die Abhängigkeit
  entfernt oder das betroffene Feature lazy geladen wird.

---

Umgesetzt in `frontend/src/app/features/import/import.a11y.spec.ts` (NfA-F1),
`frontend/src/security/no-sanitizer-bypass.spec.ts` (NfA-F2) und dem `production`-Budget in
`frontend/angular.json` (NfA-F3) — `npm test`/`npm run build` in `frontend/` liefern einen echten
Grün/Rot-Status; CI-Integration steht noch aus.
