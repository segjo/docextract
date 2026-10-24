# Entwicklungstagebuch — DocExtract

Laufendes Protokoll der KI-gestützten Entwicklung (PROJEKTPLAN.md § 9: "jede relevante
KI-Nutzung _laufend_ festhalten"). Jede Copilot-Chat-Session wird hier als ein Eintrag
zusammengefasst — Prompt-Intent, eingesetztes Tool/Modell, was übernommen, korrigiert oder
verworfen wurde, und die daraus gezogene Lehre. Dies ist die **Diff-Notiz** gemäss
PROJEKTPLAN.md Block 2 Position 2.3.

## Format pro Eintrag

```markdown
## YYYY-MM-DD — Kurztitel (Block X.Y)

**Tool/Modell:** ...
**Auftrag (Prompt-Zusammenfassung):** ...

**Übernommen:**

- ...

**Korrigiert:**

- ...

**Verworfen / Veto:**

- ...

**Reflexion:** Was war die Lehre, was würde man beim nächsten Mal anders machen?
```

> **Hinweis zu den rückwirkenden Einträgen unten (2026-09-02 bis 2026-10-11):** Diese wurden
> nachträglich aus der Copilot-Chronicle-Sitzungshistorie (gespeicherte Prompts dieses Repos)
> rekonstruiert, nicht aus Live-Beobachtung. Da für Sessions vor dem 17.10.2026 keine laufende
> Mitschrift existierte, sind sie **pro Themen-/Branch-Cluster zusammengefasst** statt pro
> einzelner Chat-Session (bei 90 protokollierten Sessions wäre eine 1:1-Erfassung unleserlich) und
> beruhen nur auf den echten Nutzer-Prompts — Detailaussagen zu "exakt was die KI im Diff geändert
> hat" sind dort bewusst allgemeiner gehalten, wo das aus den Prompts allein nicht zweifelsfrei
> rekonstruierbar war. Ab dem Eintrag vom 2026-10-17 werden Sessions wieder direkt (live) erfasst.

---

## 2026-09-02 — Projekt-Bootstrap: Skelett, Docker-Compose, Auth-Grundlagen (Block 1)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus), Branch `copilot/docextract-incoming-documents`.

**Auftrag (Prompt-Zusammenfassung):** Initiales Projektskelett aufsetzen — Doku-Verlinkung zur
Architektur, `docker-compose.yml`, Conventional-Commits-Konvention in den Instructions, Paketname
(`ch.github.segjo.docextract`), Frontend-Platzierung/Pfad-Fragen (`/adeon-docextract` Root,
API/UI-Pfade), Authentifizierung (jeder `/api`-Request über d.velop-Session validiert, Cookie
`AuthSessionId` statt eigenes Token an den Identity-Provider durchreichen).

**Übernommen:** Docker-Compose-Grundgerüst, Conventional-Commits-Regel in den Instructions,
Context-Path-/UI-Pfad-Festlegung, Cookie-basierte Session-Validierung gegen den d.velop
Identity-Provider.

**Korrigiert:** Mehrere Rückfragen im Verlauf deuten auf Nacharbeit hin — ein
`PlaceholderResolutionException`-Startfehler (Spring-Konfiguration) sowie eine Korrektur, dass
_nicht_ das Cookie selbst, sondern die daraus gelesene `AuthSessionId` an den Identity-Provider
weitergereicht werden soll.

**Verworfen / Veto:** Eigene `SecurityContextHolder`/`CustomUserDetails`-Klasse(n) wieder entfernt,
nachdem der Nutzer hinterfragte, ob sie neben `AuthContextPort` überhaupt nötig sind ("is this
class needed?", "can i remove ...") — ebenso wurde geprüft (und vermutlich verneint), ob
`spring-boot-starter-security` nach dieser Vereinfachung noch gebraucht wird.

**Reflexion:** Schon im allerersten Bootstrap-Tag zeigt sich ein wiederkehrendes Muster dieses
Projekts: KI-generierter Code wird nicht blind übernommen, sondern aktiv auf Notwendigkeit geprüft
("is this class needed?") — eine gute frühe Gewohnheit, die sich durch spätere Sessions zieht.

---

## 2026-09-04 bis 2026-09-09 — Ablaufdokumentation, Diagramme, Projektstruktur (Block 1)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus), Branch `copilot/docextract-incoming-documents`
→ `doc/fix-references`.

**Auftrag (Prompt-Zusammenfassung):** Happy-Path-Ablauf (Upload → Vorschlag) dokumentiert, C4-
Diagramm-Layout korrigiert (überlappende Labels/Linien, Block-Positionierung), Markdown-Mermaid-
Preview-Problem geklärt (Umgebungsproblem mit der VS-Code-Erweiterung, nicht KI-generiert), API-
Doc-Referenzen unter `doc/api` verlinkt, Projektstruktur gemäss aktualisierter Instructions
refaktoriert, lokale Dev-Mode-Anleitung (`./mvnw spring-boot:run -Dspring-boot.run.profiles=local`)
ergänzt, anschliessend kaputte Doku-Referenzen reparatur.

**Übernommen:** Alle genannten Doku-Korrekturen und die Dev-Mode-Anleitung.

**Korrigiert:** Ein Mermaid-Parse-Fehler ("Parse error on line 26: ... → NOTIFY(processId) ...")
in der Ablaufbeschreibung wurde nach Rückmeldung behoben.

**Reflexion:** Reine Doku-/Diagramm-Iteration ohne Produktionscode — zeigt, dass ein erheblicher
Teil der frühen KI-Nutzung auf Architektur-Kommunikation (C4, Ablaufdiagramme) entfiel, nicht nur
auf Code.

---

## 2026-09-12 bis 2026-09-17 — Ingest-Modul: Upload, DMS-Chunk-Upload, Preview, SSE (Block 1→3)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus), Branch `feat/document-upload`.

**Auftrag (Prompt-Zusammenfassung):** `ingest`-Modul nach Hexagonal-Vorgabe implementiert
(Domain/Application/Adapter), DMS-Chunk-Upload gegen d.velop, e2e-REST-Testsektion ergänzt,
`copilot-instructions` an ADR-008-Änderungen angepasst, Zugriffsbeschränkung auf Blob-Daten auf
gleiche `tenantId`/`userId`, WireMock-Recording-Setup für den `dvelop-mock`-Container, automatische
Vorschau nach erfolgreichem Upload (Gotenberg + natives PDF), SSE-Fortschrittsanzeige in
`index.html`, Thread-Sicherheit/Concurrency-Fixes nach SonarQube-Hinweisen, Byte-Range-Handling
für Preview-Streaming, CI-Workflow-Sicherheitsfunde (Gitleaks/SAST).

**Übernommen:** Ingest-Modul, DMS-Chunk-Upload, e2e-Testsektion (`e2e/ingest.http`), Tenant/User-
Ownership-Check auf Blob-Zugriff, automatische Preview-Generierung, SSE-Fortschritt in der UI.

**Korrigiert:**

- `queryForObject` konnte eine `NullPointerException` werfen — behoben.
- Ein als `volatile` markiertes, aber nicht wirklich threadsicheres Feld wurde durch einen
  echten threadsicheren Typ ersetzt (SonarQube-Fund).
- Offene Bytebereiche (`Range: bytes=100-`) wurden fälschlich zu `Long.MAX_VALUE`, statt korrekt
  behandelt — gefixt.
- MCP-Ingest funktionierte ausserhalb eines Web-Requests nicht (`DocumentBlobAdapter`
  Request-Scope-Problem) — korrigiert.
- Ein 502-Fehler beim DMS-Chunk-Upload wurde mehrfach über mehrere Sessions hinweg debuggt
  (Content-Length/Chunking-Thematik, später als generelles `Http1RestClientFactory`-Muster
  institutionalisiert, s. Repo-Memory).

**Verworfen / Veto:**

- Ein Reviewer-Fund ("stores the original before uploading to DMS, but never delet...") deutet auf
  eine erkannte, aber damals nicht sofort geschlossene Lücke (verwaiste Blobs) hin.
- `AuthContextPort`-Rückfrage ("is #sym:AuthContextPort needed when on every controller i c...")
  führte zu einer Vereinfachung der Auth-Context-Weitergabe.
- DMS- und Gotenberg-Clients nutzten `RestClient.create()` ohne explizite HTTP/1.1-Factory — als
  Risiko erkannt und (laut späterer Memory-Notiz) auf `Http1RestClientFactory` umgestellt.

**Reflexion:** Dies war der aufwendigste frühe Block — die Kombination aus echten, live gegen
d.velop/WireMock reproduzierten Fehlern (502er, Byte-Range, MCP-Request-Scope) zeigt, dass KI-
generierter Code hier wiederholt gegen die reale Gegenseite (DMS-Mock) verifiziert und mehrfach
nachgebessert wurde, statt beim ersten Entwurf zu bleiben.

---

## 2026-09-18 (vormittags) — CI/CD-Pipeline: Secret-/SAST-/Dependency-Scan (Block 5-Vorgriff)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus), Branches `main` → `feat/enhance-ci-workflow`.

**Auftrag (Prompt-Zusammenfassung):** Empfehlung für Pipeline-Gates (Secret-Scan, Opengrep-SAST,
Dependency-Scan) eingeholt, lokal mit `act` getestet, ein Gitleaks-False-Positive per
`.gitleaksignore` unterdrückt, Trivy-Scan-Ergebnis sichtbarer im Pipeline-Log gemacht (ohne
unnötigen doppelten Scan).

**Übernommen:** CI-Workflow mit Secret-/SAST-/Dependency-Scan-Jobs (später zum vollen
DevSecOps-Gate in § 9 ausgebaut, s. Repo-Memory zu `.github/workflows/ci.yml`).

**Korrigiert:** Ein zunächst doppelt laufender Trivy-Scan wurde hinterfragt ("is scan twice needed
for report???") und vermutlich auf einen Durchlauf reduziert.

**Reflexion:** Vorgriff auf Block 5 (DevSecOps-Gate) bereits in Block-1/2-Zeitraum begonnen — passt
zur PROJEKTPLAN-Empfehlung, Pflichtteile früh einzuplanen statt sie ans Ende zu schieben.

---

## 2026-09-18 bis 2026-09-22 — Structuring- & Retrieval-Schritt, DMS-Objektmetadaten (Block 3/4)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus), Branches `feat/implement-structuring-step` →
`feat/implement-retrieval-step`.

**Auftrag (Prompt-Zusammenfassung):** Structuring-Schritt implementiert (Preview/Original-PDF via
docling strukturieren, UI-Tracking ergänzt), Logging-Setup (`@Slf4j`), gemeinsame
`supports(MediaType)`-Schnittstelle für alle Adapter (Gotenberg/Docling/Preview) statt einer
zentralen Liste; danach Retrieval-Schritt (Embedding, konfigurierbarer Mindest-Score, Health-
Endpoint für Container-Konnektivität, Cluster-Statelessness-Check, DMS-Objekteigenschaften-
Anreicherung pro Treffer, Datenmodell-Design für `DmsDocumentMetadata`/`DmsPropertyDefinition`
inkl. Daten-Typ-Enum, Embedding-Tabellenspalten für Hash/Modell/Quelle, DMS-Valuelist-Webhook-
Integration).

**Übernommen:** Structuring- und Retrieval-Module, Health-Endpoint, konfigurierbarer
`min-score`, DMS-Objekteigenschaften-Anreicherung, `DmsDocumentType`/`DmsPropertyDefinition`-
Modell mit `DmsPropertyDataType`-Enum, Valuelist-Webhook.

**Korrigiert:**

- `STRUCTURED — FAILED`: docling-Strukturierung schlug zunächst fehl, debuggt und behoben.
- Eine Flyway-`FlywayValidateException` beim Hinzufügen neuer Embedding-Spalten wurde durch ein
  neues Migrationsskript statt nachträglicher Änderung bestehender Skripte gelöst.
- Mehrere 400er gegen den DMS-Valuelist-Webhook ("Formdata and content are invalid", WireMock-
  Origin-Error) wurden über mehrere Iterationen behoben (`Content-Type: text/plain`,
  Request-Felder reduziert/ergänzt).
- Ein SonarQube-Fund ("Remove this useless assignment to local variable 'body'") wurde behoben.
- PDFBox-Unicode-Warnung bei fehlender Font-Zuordnung als Rauschen erkannt (kein funktionaler Fix
  nötig).

**Verworfen / Veto:**

- `GotenbergPreviewAdapter` wurde ungefragt mitgeändert und vom Nutzer hinterfragt ("why is
  #sym:GotenbergPreviewAdapter also changed? this works with H..." ) — deutet auf eine
  zurückgenommene/korrigierte Nebenwirkungsänderung hin.
- Property-Daten wurden zunächst immer vollständig angezeigt; auf Wunsch reduziert ("dont show all
  property data, only if exists").
- Das Audit-Modul wurde in dieser Phase mit Feldern `model/mode/tokenCount` implementiert, später
  (laut Repo-Memory) als zwischenzeitlich wieder verworfen/neu aufgesetzt dokumentiert — ein
  Beispiel für eine Iteration, die in einer späteren Session nochmals neu gebaut wurde.

**Reflexion:** Der DMS-Valuelist-Webhook (undokumentierter Endpoint, nicht in der OpenAPI-Spec) war
der aufwendigste Debug-Zyklus dieser Phase — mehrere Sessions allein für 400er-Fehler gegen
WireMock/reales DMS. Lehre (auch für künftige Einträge): bei undokumentierten Drittsystem-
Endpunkten immer gegen eine echte Aufzeichnung (WireMock-Recording) verifizieren, nicht nur gegen
die (lückenhafte) OpenAPI-Spec.

---

## 2026-09-22 bis 2026-10-06 — Extraction-Schritt, Spring-AI-Umstieg, SPEC-Refactor (Block 3/4)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus), Branches `feat/implement-extraction-step` →
`refactor/move-to-spring-ai` → `main`.

**Auftrag (Prompt-Zusammenfassung):** Extraction-Schritt mit Generierungsmodell implementiert,
UI-Ergebnisanzeige (PropertyId → Name auflösen), Umstieg von direkten Ollama-REST-Adaptern auf
Spring AI (ADR-009) inkl. Azure-OpenAI-Anbindung, grosser SPEC-Konformitäts-Refactor (Chunking
entfernt zugunsten eines `content`-Moduls, ADR-011/-012), wiederholte Aufräumarbeiten
("why is locality needed? Remove it when not program logic", `application.yml`-Bereinigung,
ADR-Nummern-Konsistenzprüfung gegen den Code), OpenAPI-Artefakt-Erzeugung beim Build statt
offener Swagger-UI in Produktion.

**Übernommen:** Spring-AI-basierte LLM/Embedding-Adapter, Azure-OpenAI-Opt-in (ADR-010), der
`content`-Modul-Refactor (ADR-011/-012), build-zeit-generierte OpenAPI-Spec
(`docextract-api-docs.json`, `mvn -Popenapi-spec verify`).

**Korrigiert:**

- Ein Spring-Bean-Startfehler nach der Azure-OpenAI-Integration (`Error starting
ApplicationContext`) wurde behoben.
- `EXTRACT_SYSTEM_PROMPT` fehlte nach `CLASSIFY_SYSTEM_PROMPT` in der Doku/Implementierung — nach
  Hinweis ergänzt (verwandt mit dem später in der Memory dokumentierten
  `isModifiable`-Hardcoding-Bug derselben Codepfad-Familie).
- Ein nicht sichtbarer Button-Text (Kontrastproblem) wurde korrigiert.
- Springdoc/Swagger war im VS-Code-Debug-Start (`F5`) nicht aktiv, weil das Profil fehlte —
  behoben.

**Verworfen / Veto:**

- Mehrfach wiederholte Rückfrage **"why is locality needed? Remove it when not program logic"**
  (vier Mal in Folge im selben Chat) — ein Konfigurationsfeld/Parameter wurde als unnötige
  Komplexität identifiziert und entfernt, nicht nur einmal, sondern hartnäckig über mehrere
  Turns durchgesetzt.
- Eine doppelte Embedding-Modell-Konfiguration (`retrieval.embedding-model` und
  `embedding.model-id`) wurde als Redundanz erkannt und bereinigt.
- Die Frage "is spring boot ai needed, or can i remove it, include ADR-009?" zeigt eine
  grundsätzliche Hinterfragung der zuvor übernommenen Architekturentscheidung — Spring AI wurde
  im Ergebnis beibehalten, aber die Entscheidung erneut geprüft statt als gegeben hingenommen.

**Reflexion:** Diese Phase zeigt das stärkste Reflexions-Muster im gesamten Projekt: wiederholtes,
explizites Hinterfragen von KI-eingeführter Komplexität ("warum braucht es das?", "kann das weg?")
statt passiver Übernahme — genau das Verhalten, das PROJEKTPLAN § 9 mit den Reflexions-Punkten
belohnen will. Für künftige Einträge: solche Recall-Momente ("review the code changes") sollten
im Tagebuch explizit als eigener Diff-Review-Punkt festgehalten werden, nicht nur als Nebensatz.

---

## 2026-10-07 bis 2026-10-11 — Frontend-Grundlagen: Farbpalette, ADR-013, Angular-Skelett, Mocks (Block 2)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus), Branch `feat/implement-frontend`.

**Auftrag (Prompt-Zusammenfassung):** Farbpalette aus HTML-Mock extrahiert, FRONTEND-SPEC.md mit
CONTEXT.md verlinkt, messbare Kriterien für Barrierefreiheit/Frontend-Performance/Security
definiert (NfA-F1–F3), Ablauf aus FRONTEND-SPEC.md gemäss Layouts umgesetzt, ADR-013 (CSR statt
SSR) erstellt und in FRONTEND-SPEC.md ergänzt, Angular-22-Skelett generiert (`frontend/`), Spec-
Dokumentation von Jasmine auf Vitest korrigiert, Mock-Backend-Modus für Frontend-Entwicklung ohne
laufendes Spring-Boot-Backend ergänzt.

**Übernommen:** Farbpalette (`docextract-color-palette.html`), ADR-013, Angular-22-Skelett
(Routing, SCSS), Mock-Modus (`ng serve --configuration mock`, `mock-api.interceptor.ts`).

**Korrigiert:**

- FRONTEND-SPEC.md enthielt ursprünglich Jasmine-Beispielcode (`jasmine-axe`), was nicht zum
  tatsächlichen Angular-22-Standard-Testrunner (Vitest) passte — nach Hinweis korrigiert
  (s. auch Repo-Memory zu `vitest-axe`).
- Ein TypeScript-Importfehler (`Cannot find module './features/process/process-page'`) während
  der Skelett-Generierung wurde behoben.
- Nach Aktivierung des Mock-Modus gab es einen Pfadfehler beim Aufruf unter
  `localhost:4200/adeon-docextract/ui/...` (Base-Href/Context-Path-Mismatch im Dev-Server) —
  behoben.

**Reflexion:** Das Umschalten der Spec-Dokumentation von Jasmine auf Vitest ist ein gutes Beispiel
für einen **von der KI generierten, aber sachlich falschen Vorschlag** (Angular 22s Default-
Testrunner wurde missachtet) — vom Nutzer erkannt und korrigiert, nicht von der KI selbst bemerkt.
Das unterstreicht, warum Komponenten-Tests (wie am 2026-10-17 nachgezogen) frühzeitig mitlaufen
sollten: ein grüner `ng test`-Lauf hätte diesen Stilbruch in der Doku zwar nicht automatisch
gefunden, aber ein tatsächlicher Testlauf mit den dokumentierten Jasmine-Snippets wäre sofort
fehlgeschlagen.

---

## 2026-10-13 bis 2026-10-14 — Frontend-Prozessseite: Dokumenttyp-Wechsel, Fehlerseiten (Block 2/3)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus), lokales Arbeitsverzeichnis (kein
Branch-Metadaten-Eintrag, vermutlich `feat/implement-frontend`-Fortsetzung).

**Auftrag (Prompt-Zusammenfassung):** `import-info`-Backend-Endpoint für die Import-Maske
(Dateitypen/-grösse/Repository-Auswahl), Redesign der Import-Maske nach einem bereitgestellten
HTML-Mock, `npm run build`-Ausgabepfad auf `src/main/resources/static` korrigiert, Checkbox „leere
Eigenschaften anzeigen" ergänzt, Dialog für Dokumenttyp-Wechsel mehrfach neu spezifiziert (zunächst
ein „Erneut extrahieren"-Button, dann durch einen 3-Optionen-Dialog ersetzt: Abbrechen / wechseln
mit leeren Werten / wechseln mit Neu-Extraktion), Whitelabel-Error-Page beim Aufruf einer
Prozess-URL untersucht, Spring-Startfehler durch ungültiges Pfad-Pattern (`/ui/**/{path:[^.]*}`)
behoben, DMS-`storeDocument`-Beispielaufruf inkl. Auslesen der `documentId` aus dem
`Location`-Header integriert.

**Übernommen:** `import-info`-Endpoint, überarbeitetes Import-Layout, „leere Eigenschaften
anzeigen"-Checkbox, finaler 3-Options-Dialog für Dokumenttyp-Wechsel (exakt die Komponente, die am
2026-10-17 erstmals mit `properties-panel.spec.ts` abgedeckt wurde).

**Korrigiert:**

- `npm run build` legte die Angular-Artefakte zunächst unter `frontend/dist/docextract-ui/browser`
  statt unter `src/main/resources/static` ab — per Maven-Profil-Anpassung korrigiert (s. Repo-
  Memory zum `frontend-build`-Profil).
- Ein ungültiges Spring-MVC-Pfad-Pattern (`/ui`/\*_`/{path`:[^.]_}`) verhinderte den Appstart —
  behoben.
- Die Whitelabel-404-Seite beim Aufruf einer bestehenden Prozess-URL deutete auf das (laut
  Repo-Memory bis heute offene) Problem hin, dass es keinen GET-Endpoint zur Rekonstruktion einer
  bestehenden `processId` gibt.

**Verworfen / Veto:** Der ursprüngliche „Erneut extrahieren"-Button-Ansatz für den
Dokumenttyp-Wechsel wurde **zweimal** verworfen — erst zugunsten eines einfachen Dialogs, dann
zugunsten des finalen 3-Optionen-Dialogs (Abbrechen/leeren/neu-extrahieren), nachdem der Nutzer die
Anforderung präzisiert hatte.

**Reflexion:** Die zweifache Nachbesserung des Dokumenttyp-Wechsel-Dialogs zeigt, dass UI-
Interaktionsdesign iterativ über mehrere Prompts verfeinert wurde, statt im ersten Entwurf stehen
zu bleiben — die Komponente, die daraus entstand (`properties-panel.ts`), war entsprechend auch am
17.10. der komplexeste Testfall (Dialog-Flow mit drei Pfaden).

---

## 2026-10-17 — PROJEKTPLAN-Statusprüfung Block 2 + Unit-Tests 2.3 (Block 2.3/2.6)

**Tool/Modell:** GitHub Copilot Chat (Claude Sonnet 5), Agent-Modus mit Workspace-Tools
(Datei-Suche/-Lese-/-Schreibzugriff, Terminal).

**Auftrag (Prompt-Zusammenfassung):**

1. PROJEKTPLAN.md aktualisieren und prüfen, wie weit Block 2 (Frontend) erfüllt ist.
2. Die für Position 2.3 fehlenden Unit-Tests nachziehen.
3. Optionen für eine Diff-Notiz zu KI-Korrekturen/-Verwürfen aufzeigen.
4. Dieses Entwicklungstagebuch anlegen und die laufende Session als ersten Eintrag erfassen.

**Übernommen:**

- Bestandsaufnahme von Block 2 (2.1–2.6) anhand des tatsächlichen Repo-Zustands (nicht nur
  SPEC/ARCHITECTURE-Beschreibung) — Wireframes, Angular-Implementierung, A11y-/Security-/
  Performance-Specs und ADR-013 (CSR) wurden als erfüllt bestätigt.
- 11 neue Spec-Dateien (51 Tests) für bis dahin ungetestete Komponenten/Module generiert:
  `process-progress.spec.ts`, `attribute-review.spec.ts`, `status-bar.spec.ts`,
  `protocol-panel.spec.ts`, `similar-panel.spec.ts`, `properties-panel.spec.ts`,
  `document-viewer.spec.ts`, `process-page.spec.ts` — inkl. Dialog-Flows, Output-Events und
  API-Fehlerpfaden. Alle Tests grün, `ng lint` sauber.
- Vorschlag, dieses Tagebuch (statt nur Commit-Trailer oder Chat-Export) als primäres
  Diff-Notiz-Artefakt zu führen, weil es direkt auf PROJEKTPLAN.md § 9 einzahlt.

**Korrigiert:**

- Erster Entwurf von `attribute-review.spec.ts` hatte eine falsche Testerwartung
  (`toReview: 1` erwartet, aber die editierten Testdaten entsprachen nicht dem geprüften
  Pfad) — nach einem fehlgeschlagenen Testlauf anhand der tatsächlichen Funktionslogik
  (`isReviewedAttribute`) korrigiert.
- `frontend/node_modules` fehlte zweimal im Verlauf der Session die native `oxc-parser`-Binding
  (bekanntes `npm i`-Optional-Deps-Problem, s. Repo-Memory) — jeweils durch erneutes `npm i`
  behoben, kein Code-Problem.

**Verworfen / Veto:**

- Der ursprüngliche Testentwurf für den Fehlerpfad von `ProcessPage.loadResults()` wurde
  **verworfen**: er löste (über das reale `EXTRACTED`-Event + einen dauerhaft fehlschlagenden
  API-Mock) die reale Retry-`effect()`-Logik aus — das liess den Vitest-Worker mit
  `SIGABRT`/Out-of-Memory abstürzen, keine normale Testfehlschlagsmeldung. Der Test wurde
  stattdessen so umgebaut, dass `loadResults()` direkt aufgerufen wird, um die
  Endlosschleife nicht zu reproduzieren.

**Reflexion:**
Das Schreiben der Komponenten-Tests hat einen **echten, bis dahin unentdeckten Produktionsfehler**
aufgedeckt: `ProcessPage`s Retry-`effect()` (`isExtractionComplete() && !resultsReady() &&
!loadingResults()`) terminiert nie, wenn `extraction-result` dauerhaft fehlschlägt — ein
Endlos-Retry gegen das Backend. Das ist ein gutes Beispiel dafür, dass generierte Unit-Tests nicht
nur Abdeckung liefern, sondern beim Verfassen selbst Fehler im Zielcode sichtbar machen (hier durch
einen Crash statt eines stillen Fehlschlags) — der Fund wurde dem Nutzer gemeldet, aber bewusst
**nicht** ungefragt im Produktionscode gefixt (Scope-Disziplin: nur das umsetzen, was explizit
verlangt wurde). Lehre: bei signalbasierten Angular-`effect()`-Retry-Mustern immer einen expliziten
"permanent failed"-Zustand vorsehen, der die Re-Trigger-Bedingung bricht.

---

## 2026-10-17 — Rückwirkender Tagebuch-Backfill aus der Chronicle-Sitzungshistorie

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus), Chronicle-Sitzungsabfrage (`session_store_sql`,
DuckDB-Query gegen die gespeicherte Cross-Session-Historie dieses Repos).

**Auftrag (Prompt-Zusammenfassung):** Prüfen, ob auf andere (vergangene) Chat-Sessions zugegriffen
werden kann, um das neu angelegte Tagebuch rückwirkend zu befüllen; falls ja, Umfang des Backfills
festlegen und umsetzen.

**Übernommen:**

- Zugriff auf 90 protokollierte Sessions dieses Repos (2026-09-02 bis heute) über die Chronicle-
  Sitzungstabellen (`sessions`, `turns`) bestätigt und für den Backfill genutzt.
- 13 rückwirkende Sammel-Einträge (2026-09-02 bis 2026-10-14) auf Basis der echten gespeicherten
  Nutzer-Prompts erstellt und oberhalb dieses Eintrags chronologisch eingefügt — pro
  Themen-/Branch-Cluster statt pro Einzelsession, um die Lesbarkeit zu erhalten.
- Einen erklärenden Hinweis-Absatz ergänzt, der offenlegt, dass diese Einträge rekonstruiert
  (nicht live beobachtet) sind und wo die Detailtiefe dadurch geringer ist.

**Korrigiert:** Die erste Abfrage nach `workspace_path`/`DESCRIBE sessions` schlug fehl (Spalte
existiert nicht bzw. `DESCRIBE` ist in diesem Read-only-SQL-Modus nicht erlaubt) — über
`information_schema.columns` die tatsächlichen Spalten ermittelt und die Query entsprechend
angepasst. Eine Datumsfilter-Query lieferte zudem nur 100 von 104 Zeilen (Tool-Output-Trunkierung)
— als bekannte Grenze akzeptiert statt mit Gewalt vollständig nachzuladen (geringer Zusatznutzen
für 4 fehlende Prompt-Zeilen gegenüber dem Aufwand).

**Verworfen / Veto:** Keine 1:1-Erfassung aller 90 Einzelsessions (wäre technisch möglich gewesen,
aber als unleserlich/unverhältnismässig verworfen) — stattdessen die oben beschriebene
Cluster-Verdichtung gewählt.

**Reflexion:** Rückwirkende KI-Nutzungs-Dokumentation ist nur so gut wie die Einordnung ihrer
eigenen Unsicherheit — deshalb der explizite Hinweis-Absatz am Dateianfang, der zwischen "live
mitgeschrieben" (ab 2026-10-17) und "aus Prompt-Historie rekonstruiert" (davor) unterscheidet.
Lehre für zukünftige Projekte: ein Tagebuch von Anfang an laufend führen ist deutlich günstiger als
ein nachträglicher Chronicle-Backfill, selbst wenn Letzterer technisch möglich ist.

---

## 2026-10-24 — Fallback bei `dmsDocumentId == null` in `ConfirmAndWriteBackService` (Block 2.3/3)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus), Workspace-Tools (Datei-Suche/-Lese-/
-Schreibzugriff, `mvn`-Terminal).

**Auftrag (Prompt-Zusammenfassung):** In `ConfirmAndWriteBack#confirm` einen Fallback für den Fall
implementieren, dass `dmsWritePort.writeAttributes(...)` eine `null` `dmsDocumentId` liefert — das
soll bedeuten, dass die DMS-Attribute fachlich falsch sind, und der Validierungsschritt muss
wiederholt werden (statt die Saga mit einer kaputten `dmsDocumentId` fortzusetzen).

**Übernommen:**

- Früher `null`-Check direkt nach dem DMS-Write in `ConfirmAndWriteBackService#confirm`: bricht die
  Saga **vor** dem Erfolgs-Audit-Event und vor `corpusPromotionPort.promote(...)` ab, loggt
  stattdessen ein `validation.writeback-rejected`-Audit-Event und wirft eine neue, dedizierte
  `InvalidAttributesException`.
- `InvalidAttributesException` als Pendant zu `ConsentRequiredException` angelegt und im
  bestehenden `GlobalExceptionHandler` auf `422 UNPROCESSABLE_ENTITY` mit sprechendem
  `ProblemDetail`-Titel ("Invalid attributes — validation must be repeated") gemappt — dasselbe
  Muster wie beim bereits vorhandenen Consent-Handler.
- `ConfirmAndWriteBackServiceTest` um einen neuen Testfall ergänzt (`null`-Rückgabe von
  `writeAttributes` → `InvalidAttributesException`, `corpusPromotionPort.promote` wird **nicht**
  aufgerufen) sowie den bestehenden Test auf die aktuelle 3-Parameter-Signatur von
  `DmsWritePort#writeAttributes` nachgezogen.

**Korrigiert:** Der volle `mvn compile`/`mvn test` schlug zunächst an einer **fremden,
unabhängigen** Baustelle fehl: `DmsWriteAdapter` (Platzhalter-Adapter) hatte durch eine bereits im
Arbeitsverzeichnis vorhandene, unfertige Signaturänderung (`void` → `String`) keine
`return`-Anweisung mehr und liess das Modul nicht kompilieren. Um die eigene Änderung dennoch zu
verifizieren, wurde der Adapter **temporär** lokal mit `return null;` kompilierfähig gemacht, die
Tests liefen grün, und der Adapter anschliessend exakt auf den ursprünglichen (kaputten,
unveränderten) Stand zurückgesetzt — bewusst kein Fix dieser fremden Baustelle, da ausserhalb des
Auftragsumfangs.

**Verworfen / Veto:** Keines — der Scope blieb bewusst auf `ConfirmAndWriteBackService`, die neue
Exception-Klasse, den Exception-Handler und den zugehörigen Test beschränkt; `DmsWriteAdapter`
wurde nicht "nebenbei mitgefixt".

**Reflexion:** Gute Illustration der Scope-Disziplin-Regel "keine unverwandten, vorbestehenden
Probleme fixen": Der kaputte `DmsWriteAdapter` hätte sich leicht "miterledigen" lassen (eine Zeile),
aber da er nicht durch die eigene Änderung verursacht wurde und zu einer erkennbar separaten,
laufenden Baustelle gehört, wurde die Verifikation stattdessen über eine **temporäre, wieder
rückgängig gemachte** Stub-Änderung erreicht statt über einen bleibenden Fremd-Fix. Lehre: bei
halbfertigen, uncommitteten Nachbar-Dateien lohnt sich vor dem Testlauf ein kurzer `git diff`-Check,
um vorbestehende von selbst verursachten Kompilierfehlern zu unterscheiden.
