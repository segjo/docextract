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

---

## 2026-11-05 — Port-Namen in ARCHITECTURE.md an § 5.2 und Code angleichen (Block 3)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus), Branch `feat/coding-agent`.

**Auftrag (Prompt-Zusammenfassung):** Widersprüche in `doc/ARCHITECTURE.md` beheben, ohne Inhalte
fachlich zu ändern: (1) § 5.4 statt `VectorStorePort`/`DmsReadPort` auf die schmalen Ports aus
§ 5.2/§ 5.5 umstellen, (2) Modulzuordnung von `ValueListPort`, `DmsObjectDefinitionPort`,
`DmsObjectMetadataPort` am Code prüfen und § 5.4/§ 5.5 daran angleichen, (3) in § 5.2 genannte,
im Code fehlende Ports auflisten (nicht implementieren). Akzeptanz: kein Port-Name in
ARCHITECTURE.md, der weder in § 5.2 noch im Code existiert.

**Geänderte Dateien:** `doc/ARCHITECTURE.md`, `doc/ENTWICKLUNGSTAGEBUCH.md`. Kein Code geändert
(ArchUnit/`mvn verify` dadurch nicht betroffen).

**Übernommen (generiert):**

- § 5.2: Modulzuordnung ergänzt — `DmsObjectDefinitionPort`/`DmsObjectMetadataPort` (retrieval),
  `ValueListPort` (retrieval, von extraction mitgenutzt).
- § 5.4: retrieval → `EmbeddingPort`, `PendingEmbeddingPort`, `VectorSearchPort`,
  `DmsObjectDefinitionPort`, `DmsObjectMetadataPort`, `ValueListPort`, `AuthContextPort`;
  extraction → `LlmPort`, `ValueListPort` (aus retrieval), `DocumentTypeCandidatesPort` (aus
  retrieval); validation → `DmsWritePort`, `CorpusPromotionPort`, `PendingEmbeddingDeletePort`.
  Tabelle per Prettier neu ausgerichtet.
- § 5.5: `DmsValueListWebhookAdapter` unter `retrieval/adapter.out/dms` ergänzt; Hinweis bei
  `extraction/port`, dass `ValueListPort` aus `retrieval.port` mitgenutzt wird.
- § 6.1: Participant `retrieval (EmbeddingPort / VectorStorePort)` →
  `(EmbeddingPort / PendingEmbeddingPort / VectorSearchPort)` (nötig für das Akzeptanzkriterium).
- Abschnittsnummern unverändert.

**Abweichung Code ↔ Doku (Doku folgt dem Code):** Alle drei Ports liegen im Code in
`ch.adeon.apps.docextract.retrieval.port`. Genutzt werden `DmsObjectDefinitionPort`/
`DmsObjectMetadataPort` nur von `retrieval` (`FindSimilarService`, `GetObjectDefinitionsService`);
`ValueListPort` zusätzlich von `extraction.application.ExtractAttributesService`. Die frühere
§ 5.4-Zuordnung „extraction → `DmsReadPort`" entsprach nicht dem Code: extraction liest objdef/
Top-5-Metadaten nicht selbst aus dem DMS, sondern über `retrieval.application.DocumentTypeCandidatesPort`.

**Ports aus § 5.2 ohne Entsprechung im Code:**

| Port               | in Doku | im Code                                                                                                |
| ------------------ | ------- | ------------------------------------------------------------------------------------------------------ |
| `PreviewPort`      | § 5.2   | nein — nächster Kandidat: `ingest.port.PreviewRenderPort`                                              |
| `ProcessStatePort` | § 5.2   | nein — vorhanden: `process.application.ProcessEventPort`/`ProcessDocumentPort`/`ProcessRepositoryPort` |
| `AuditLogPort`     | § 5.2   | nein — nächster Kandidat: `audit.application.AuditPort`                                                |

Alle übrigen § 5.2-Ports existieren im Code.

**Verworfen / Veto:** Keine Umbenennung im Code und keine Umbenennung der drei fehlenden Ports in
der Doku — beides wäre eine Entscheidung, nicht nur eine Angleichung (siehe OFFEN-Punkte).

**Zu prüfen durch Mensch:**

- OFFEN: `PreviewPort` (Doku) vs. `PreviewRenderPort` (Code) — Doku oder Code umbenennen?
- OFFEN: `ProcessStatePort` (Doku) — welchem der Code-Ports `ProcessEventPort`/`ProcessDocumentPort`/
  `ProcessRepositoryPort` entspricht er, bzw. soll § 5.2/§ 5.4 diese stattdessen nennen?
- OFFEN: `AuditLogPort` (Doku) vs. `AuditPort` (Code) — Doku oder Code umbenennen?
- OFFEN: Sollen die Modulzuordnungen `DmsObjectDefinitionPort`/`DmsObjectMetadataPort`/
  `ValueListPort` → retrieval dauerhaft so bleiben, oder ist eine Verschiebung nach extraction
  (ursprüngliche Doku-Intention) gewünscht?
- OFFEN: § 6.1 zeigt `EXT->>DMS: objdef + Metadaten Top-5 (live)`; im Code ruft `retrieval`
  (`FindSimilarService`) die DMS-Leseports auf. Laufzeitsicht anpassen? (bewusst nicht geändert, da
  Verhalten statt Port-Name)
- OFFEN: Im Code existierende, in § 5.2 nicht gelistete Ports (`DocumentTypeCandidatesPort`,
  `DmsRepositoryPort`, `ProcessRepositoryPort` sowie interne Staging-Ports) in § 5.2 aufnehmen?
- Prüfen: neue § 5.4-Zeile extraction nennt `DocumentTypeCandidatesPort` — fachlich korrekt als
  Ersatz für `DmsReadPort`?

**Reflexion:** Ein Akzeptanzkriterium über die ganze Datei („kein Port-Name ausserhalb § 5.2/Code")
fand eine zusätzliche Fundstelle in § 6.1, die im Auftrag nicht explizit genannt war — per
Skript (`grep` aller `*Port`-Namen gegen § 5.2 und `src/main/java`) statt per Augenmass prüfen.

---

## 2026-11-05 — OFFEN-Punkte der Port-Angleichung umsetzen (Block 3)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus), Branch `feat/coding-agent`.

**Auftrag (Prompt-Zusammenfassung):** Entscheide des Menschen zu den OFFEN-Punkten des
vorherigen Eintrags umsetzen: `PreviewRenderPort` und `AuditPort` (Code) behalten, Doku anpassen;
`ProcessStatePort` in der Doku durch die Code-Ports ersetzen; DMS-Ports nur nach extraction
verschieben, falls fachlich besser; § 6.1 anpassen (LLM soll dynamische/abhängige Wertelisten aus
dem DMS nachladen können); alle Code-Ports in § 5.2 aufnehmen.

**Geänderte Dateien:** `doc/ARCHITECTURE.md`, `doc/ENTWICKLUNGSTAGEBUCH.md`. Kein Code geändert.

**Übernommen (generiert):**

- § 5.2: `PreviewPort` → `PreviewRenderPort`. Die Fliesstext-Aufzählung der internen Ports ist
  durch zwei Tabellen ersetzt (Port | Modul (Paket) | Zweck): Outbound-Ports und
  Staging-/Übergabe-Ports. Neu aufgenommen: `DmsRepositoryPort`, `OutboundCredentialPort`,
  `IdentityProviderPort`, `AppConfigPort`, `AuditPort`, `ProcessEventPort`, `ProcessDocumentPort`,
  `ProcessRepositoryPort`, `CorpusMaintenancePort`, `TextStagingPort`, `ContentStagingPort`,
  `DocumentHashStagingPort`, `RetrievalResultPort`, `DocumentTypeCandidatesPort`,
  `ExtractionResultPort`. Entfernt: `ProcessStatePort`, `AuditLogPort`.
- § 5.4: ingest → `PreviewRenderPort` (+ `DmsRepositoryPort`); process → `ProcessEventPort`,
  `ProcessDocumentPort`, `ProcessRepositoryPort`; audit → `AuditPort`.
- § 5.5: `PreviewRenderPort` bei ingest; Ports bei process und audit ergänzt.
- § 6.1: DMS-Lesezugriffe (Metadaten Top-5, Wertelisten, objdef-Fallback) liegen jetzt bei
  retrieval, wie im Code (`FindSimilarService`). extraction lädt Kandidaten/Top-5 aus den
  Staging-Ports und lädt bei Bedarf Wertelisten über `ValueListPort` nach (`opt`-Block).
  Kurzer Hinweis unter dem Diagramm: Nachladen erfolgt durch `ExtractAttributesService` anhand der
  LLM-Antwort, nicht per LLM-Tool-Call (T-1, C-3), nur lesend (C-2), sonst „unbekannt" (E-5).
- Prüfung beidseitig per Skript: jeder Port-Name in ARCHITECTURE.md existiert im Code und in
  § 5.2, und jeder `*Port.java` im Code steht in § 5.2.

**Verworfen / Veto:**

- Keine Verschiebung von `DmsObjectDefinitionPort`/`DmsObjectMetadataPort`/`ValueListPort` nach
  extraction. Begründung aus dem Code: `FindSimilarService` (retrieval) braucht die ersten beiden
  vor der Extraktion (Anreicherung der Treffer, objdef-Fallback, Dokumenttyp-Kandidaten), und
  `DmsObjectMetadataAdapter` (retrieval) nutzt `ValueListPort` bereits beim Anreichern. Ein Umzug
  nach `extraction.port` würde retrieval von extraction abhängig machen, also gegen die
  Pipeline-Richtung. extraction nutzt `ValueListPort` weiterhin aus `retrieval.port` (ArchUnit
  erlaubt modulübergreifende `port`/`application`-Imports).

**Zu prüfen durch Mensch:**

- OFFEN: Soll das LLM Wertelisten künftig selbst per Tool-Call nachladen können? Das widerspräche
  C-3 („LLM hat keinen direkten Zugriff auf die DMS-API") und T-1 („keine aus Inhalt ableitbaren
  Tool-Calls") und bräuchte ein ADR. Der aktuelle Stand (Nachladen durch die Application-Schicht)
  ist so dokumentiert.
- OFFEN: `ProcessRepositoryPort` hat nur einen In-Memory-Adapter
  (`InMemoryProcessRepositoryAdapter`). Ist das im Cluster-Modus (ADR-004) ausreichend, solange ein
  Job Ende-zu-Ende auf einer Instanz läuft? (nur festgestellt, nicht geändert)
- Prüfen: Die Zweck-Spalten der neuen § 5.2-Tabellen sind aus den Interface-Signaturen und
  Adaptern abgeleitet.

**Reflexion:** Der Abgleich in beide Richtungen (Doku → Code und Code → Doku) macht § 5.2 zur
vollständigen Port-Liste; per Skript wiederholbar.

---

## 2026-11-05 — `ProcessRepositoryPort` cluster-fähig in Postgres (Block 3)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus), Branch `feat/coding-agent`.

**Auftrag (Prompt-Zusammenfassung):** (1) Das LLM soll Wertelisten über DocExtract nachladen können
(kein MCP). (2) Der In-Memory-Adapter für `ProcessRepositoryPort` reicht nicht, die Ablage soll in
PostgreSQL erfolgen. Nachgeschärft: keine neue Tabelle, die `repository_id` soll beim bestehenden
Prozess-Datensatz liegen. Gewählt per Rückfrage: ORIGINAL-Zeile in `document_blob` (wie
`dms_location`/`filename` aus V12) statt `process_step`, weil `process_step` ein Event-Log mit
mehreren Zeilen pro Prozess ist.

**Geänderte Dateien:**

- neu: `V13__document_blob_repository.sql` (Spalte `document_blob.repository_id`),
  `process/adapter/out/postgres/PgProcessRepositoryAdapter.java`,
  `PgProcessRepositoryAdapterTest.java` (Testcontainers, 3 Tests)
- gelöscht: `process/adapter/out/memory/InMemoryProcessRepositoryAdapter.java`
- geändert: `IngestDocumentService` (`stage` läuft jetzt per `thenApply` direkt nach dem
  Blob-Insert und vor allen abhängigen Schritten), Javadoc `ProcessRepositoryPort`

**Übernommen:** Gleiches Muster wie `PgProcessDocumentAdapter`, TTL/Cleanup über den Blob. Der
Bestätigungs-Request liest die `repository_id` damit auf jeder Instanz (ADR-004). Vorher fiel er
auf einer fremden Instanz still auf das Default-Repository zurück. `mvn -o clean verify` ist grün,
inkl. `HexagonalArchitectureTest`.

**Korrigiert:** Ein erster Entwurf mit eigener Tabelle `process_repository` wurde nach dem Hinweis
des Menschen verworfen. Ein Commit erfolgte nicht.

**Verworfen / Veto:** Punkt (1) ist **nicht** umgesetzt. Ein LLM-initiierter Tool-Call widerspricht
SPEC C-2 („Keine LLM-Ausgabe löst selbsttätig einen Schreib-/Tool-Call aus") und T-1 („keine aus
Inhalt ableitbaren Tool-Calls"). Das braucht einen Entscheid des Menschen bzw. ein ADR.

**Zu prüfen durch Mensch:**

- OFFEN: LLM-Tool-Call „Werteliste nachladen" gegen C-2/T-1. Soll C-2 auf schreibende Tool-Calls
  eingeschränkt werden (ADR + SPEC-Änderung), mit Mitigationen wie nur lesend, Allowlist auf die
  Properties des gewählten Dokumenttyps, `repositoryId`/Credentials serverseitig, Max-Anzahl Calls,
  Audit je Call (C-4)?
- OFFEN: Es existiert kein TTL-Cleanup-Job, der abgelaufene `document_blob`-Zeilen löscht (gilt
  bereits für alle `expires_at`-Tabellen). Das ist eine vorbestehende Lücke.

**Reflexion:** „Prozess-Tabelle" war mehrdeutig. Eine kurze Rückfrage mit zwei konkreten Optionen
war günstiger als eine falsche Migration.

---

## 2026-11-05 — Lesender LLM-Tool-Call für Wertelisten (ADR-014) + TODO Cleanup-Job (Block 3)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus), Branch `feat/coding-agent`.

**Auftrag (Prompt-Zusammenfassung):** Entscheid des Menschen: Ein lesender LLM-Tool-Call soll möglich
sein (Hinweis: der lesende Wertelisten-Call ist ein HTTP `POST`). Den fehlenden TTL-Cleanup-Job als
TODO markieren.

**Geänderte Dateien:**

- Code: `extraction/domain/LlmTool.java` (neu), `extraction/port/LlmPort.java` (4-Argument-`generate`
  mit Tools, 3-Argument-Variante als Default), `OpenAiCompatibleLlmAdapter` (`LlmTool` →
  Spring-AI-`ToolCallback`), `extraction/application/ValueListLookupTool.java` (neu),
  `ExtractAttributesService` (Tool angeboten, System-Prompt ergänzt),
  `PgDocumentBlobAdapter` (TODO-Kommentar), `application.yml`
  (`docextract.extraction.max-tool-calls: 10`)
- Test: `ValueListLookupToolTest.java` (neu, 4 Tests)
- Doku: `ARCHITECTURE.md` (ADR-014 neu; § 5.2 `LlmPort`/`LlmTool`, § 5.5 TODO, § 6.1 Tool-Loop im
  Diagramm + Hinweis, § 8.2, ADR-003-Status, § 10.3, § 11 TODO), `SPEC.md` (C-2, T-1), `CONTEXT.md`
  (C-2, T-1)

**Übernommen (generiert):**

- Das Tool `lookup_value_list` wird nur angeboten, wenn eine beschreibbare Eigenschaft eine
  Werteliste hat. Guardrails sitzen im Code, nicht im Prompt:
  - Allowlist auf die Wertelisten-Eigenschaften des gewählten Typs; Filter nur auf beschreibbare
    Nicht-System-Eigenschaften.
  - Repository, Objektdefinition und Credential setzt der Server.
  - Höchstens N Calls je Extraktion.
  - Audit je Call nur mit SHA-256 der Argumente (C-4).
  - Fehler gehen als JSON-`error` an das LLM zurück, nicht als Exception.
- Die bestehende serverseitige Prüfung der Vorschläge gegen die Werteliste (`resolveValueLists`)
  bleibt als zweite Linie bestehen.
- `POST` ≠ Write: Im ADR ist festgehalten, dass die Semantik zählt. `ValueListPort` hat keine
  schreibende Methode.
- Spring-AI-API per `javap` gegen die lokalen 2.0.0-Jars geprüft: `ToolCallback`, `ToolDefinition`,
  `OpenAiChatOptions.Builder#toolCallbacks`. `OpenAiChatModel.Builder` erzeugt standardmässig einen
  `DefaultToolCallingManager`.
- `mvn -o clean verify` grün (67 Tests, inkl. ArchUnit).

**Verworfen / Veto:** Kein MCP. Keine schreibenden Tools.

**Zu prüfen durch Mensch:**

- Nicht gegen ein echtes LLM getestet: Unterstützen die konfigurierte Runtime bzw. das Modell
  Tool-Calls zusammen mit `response_format: json_schema`? Bitte einmal live mit dem aktuellen
  Foundry/Ollama-Setup prüfen.
- `promptTokenCount` im Audit stammt aus der letzten Antwort der Tool-Schleife. Ob Spring AI die
  Usage über alle Runden aufsummiert, ist nicht verifiziert (NfA-7).
- TODO: TTL-Cleanup-Job fehlt (ARCHITECTURE § 11, § 5.5, Kommentar in `PgDocumentBlobAdapter`).

**Reflexion:** Die Guardrail-Änderung (C-2/T-1) kam erst nach dem expliziten Entscheid des Menschen
und ist als ADR dokumentiert, nicht still im Code. Die Schutzmechanismen liegen im Tool-Code und sind
dort testbar, statt sich auf Prompt-Anweisungen zu verlassen.

---

## 2026-11-05 — Bugfix: „no content returned" nach LLM-Tool-Call (Block 3)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus), Branch `feat/coding-agent`.

**Auftrag (Prompt-Zusammenfassung):** Live-Fehler beim ersten Lauf mit ADR-014:
`LlmException: no content returned for model gpt-5.6-sol` in `suggestAttributes`.

**Ursache:** Meine Annahme aus dem vorherigen Eintrag war falsch. Der `DefaultToolCallingManager`
von `OpenAiChatModel` wird in Spring AI 2.0 nur zum Auflösen der Tool-Definitionen genutzt;
`call()` führt Tool-Calls **nicht** mehr selbst aus. Per `javap` geprüft: `internalCall` ruft nur
`resolveToolDefinitions`, nie `executeToolCalls`. Das Modell lieferte deshalb nur einen Tool-Call
ohne Text zurück.

**Geändert:** `OpenAiCompatibleLlmAdapter` führt die Tool-Schleife jetzt selbst aus. Solange
`response.hasToolCalls()` gilt: `ToolCallingManager.executeToolCalls(prompt, response)`, dann erneuter
Call mit der Konversations-History. Als Sicherheitsnetz gilt ein Limit von 20 Runden (T-4); das Tool
selbst begrenzt weiterhin auf `max-tool-calls`. Die Prompt-Tokens werden jetzt über alle Runden
summiert, damit ist der NfA-7-Prüfpunkt aus dem vorherigen Eintrag erledigt.
`mvn -o clean verify` ist grün (67 Tests).

**Zu prüfen durch Mensch:**

- Erneuter Live-Lauf mit `gpt-5.6-sol`: Akzeptiert der Endpunkt `tools` zusammen mit
  `response_format: json_schema`, und kommt nach den Tool-Runden JSON-Text zurück?
- Es gibt keinen automatisierten Test für die Tool-Schleife im Adapter, weil der Adapter das
  `OpenAiChatModel` intern baut. Mittelfristig per WireMock-Stub des Chat-Endpunkts abdecken?

**Reflexion:** „Builder erzeugt einen ToolCallingManager" wurde vorschnell als „Tools werden
ausgeführt" gelesen. Bytecode-Prüfung auf den tatsächlichen Aufruf (`executeToolCalls`) statt nur
auf das Feld hätte den Fehler vor dem Live-Lauf gefunden.

---

## 2026-11-05 — TRANSFER-Entscheid Freigabe-Saga (ADR-015) + `FINALIZED_INDEX_PENDING` (Block 3)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus), Branch `feat/coding-agent`.

**Auftrag (Prompt-Zusammenfassung):** Block-3-Prompt P1: neues ADR (Status, Kontext, Entscheidung,
Preis, Störfall, Prüfung, Alternativen, Konsequenz) für die Naht validation → retrieval
(`CorpusPromotionPort`) bzw. validation → DMS (`DmsWritePort`). Danach im Code prüfen, ob
`ConfirmAndWriteBackService` die Reihenfolge DMS-Write → Promotion einhält und bei Promotion-Fehler
`FINALIZED_INDEX_PENDING` setzt, sonst implementieren; Tests a–d; Testnamen im ADR eintragen.

**Befund vorher:** Die Reihenfolge stimmte, aber `FINALIZED_INDEX_PENDING` existierte nur in der
Doku. Ein Promotion-Fehler warf einfach eine Exception (500), ein erneuter Confirm hätte den
DMS-Write wiederholt. Retry und TTL-Cleanup gab es nicht.

**Geänderte Dateien:**

- neu: `V14__process_finalization.sql`, `validation/domain/Finalization.java`,
  `validation/domain/FinalizationStatus.java`, `validation/application/FinalizationStatePort.java`,
  `validation/application/RetryCorpusPromotion.java`,
  `validation/application/CleanupExpiredPendingEmbeddings.java`,
  `validation/application/CleanupExpiredPendingEmbeddingsService.java`,
  `validation/adapter/out/postgres/PgFinalizationStateAdapter.java`,
  `validation/adapter/in/scheduler/FinalizationRecoveryScheduler.java`
- geändert: `ConfirmAndWriteBackService` (Zustand vor der Promotion speichern, Promotion-Fehler
  abfangen, Wiedereintritt, `retryPendingPromotions`), `PendingEmbeddingDeletePort` +
  `PgPendingEmbeddingDeleteAdapter` (`findExpiredProcessIds`, `deleteExpired` mit SQL-Guard),
  `application.yml` (`docextract.validation.recovery-interval: PT5M`)
- Tests: `ConfirmAndWriteBackServiceTest` (1 → 7 Tests), neu
  `CleanupExpiredPendingEmbeddingsServiceTest` (1), `PgPendingEmbeddingDeleteAdapterTest`
  (3, Testcontainers)
- Doku: `ARCHITECTURE.md` (ADR-015 neu; § 5.2, § 5.4, § 5.5, § 6.2, § 11 ergänzt)

**Übernommen (generiert):**

- ADR-015 mit den geforderten Abschnitten. Die Testklassen und Testmethoden stehen unter
  „Prüfung".
- Saga: DMS-Write → `FINALIZED_INDEX_PENDING` speichern → Promotion → `COMPLETED`. Der Zustand
  wird **vor** der Promotion gespeichert, damit auch ein Instanzausfall danach nur noch zur
  Promotion führt.
- Retry auf zwei Wegen: erneuter Confirm (Wiedereintritt) und ein Recovery-Job. Beide rufen den
  `DmsWritePort` nicht auf.
- TTL-Cleanup für `PENDING`-Embeddings: überspringt `FINALIZED_INDEX_PENDING` im Service (Test c) und
  zusätzlich im `DELETE` per `NOT EXISTS` (Race zwischen Lookup und Delete).
- Test d bestand bereits (`PgVectorSearchAdapterTest.finds_only_approved_embeddings_for_the_caller_s_tenant`)
  und ist im ADR referenziert, nicht dupliziert.
- `mvn -o clean verify` grün, inkl. `HexagonalArchitectureTest`.

**Korrigiert:** Die ADR-Nummer. Der Prompt verlangt „ADR-014", diese Nummer ist aber schon durch
„Lesende LLM-Tool-Calls" vergeben und in SPEC/CONTEXT referenziert. Nummern sollen nicht verschoben
werden, daher ADR-015.

**Verworfen / Veto:** Kein Event/Listener in `retrieval` (Choreografie, siehe ADR-015). Keine
Änderung am REST-Vertrag von `/validation/confirm`.

**Zu prüfen durch Mensch:**

- OFFEN: ADR-Nummer. Der Prompt (und `Block-3-Prompts.md`) nennt ADR-014, vergeben wurde ADR-015.
  Soll `Block-3-Prompts.md`/die Abgabe auf ADR-015 verweisen?
- OFFEN: Der TRANSFER-Entscheid muss laut `Block-3-Prompts.md` der eigene sein. Status steht auf
  „vorgeschlagen", bitte Inhalt bestätigen.
- OFFEN: Ist der DMS-Write (`DmsWriteAdapter`) idempotent? Fällt die Instanz zwischen DMS-Antwort
  und dem Speichern von `FINALIZED_INDEX_PENDING` aus, wiederholt ein erneuter Confirm den Write.
  Braucht es einen Zustand vor dem Write?
- OFFEN: Antwort von `/validation/confirm` bei Promotion-Fehler. Aktuell `200` mit
  `writtenBack = true` (DMS ist final); der Index-Status ist für Client/UI nicht sichtbar. Soll er
  im Vertrag erscheinen?
- OFFEN: Retry-Intervall (`recovery-interval: PT5M` ist ein Platzhalter), Obergrenze und Backoff
  für Promotion-Retries; ab wann gilt ein Prozess als „endgültiger Fehler" (SPEC C-7)?
- OFFEN: Aufbewahrung der Zeilen in `process_finalization` (nur IDs, keine TTL). Gehört sie in die
  Löschkaskade (NfA-5)?
- OFFEN: `RejectSuggestion` prüft den Saga-Zustand nicht. Ein Reject nach erfolgreichem DMS-Write
  würde das `PENDING`-Embedding löschen. Soll Reject in `FINALIZED_INDEX_PENDING` abgewiesen werden?
- OFFEN: § 6.2 „PENDING prüfen (vorhanden, gültig, gleicher Tenant)" vor dem DMS-Write und „Blobs
  löschen" nach der Promotion sind weiterhin nicht implementiert.
- OFFEN: § 5.5 sieht den `TtlCleanupJob` in `shared` vor. Der Embedding-Cleanup liegt jetzt in
  `validation`, weil er den Saga-Zustand braucht. Blob- und Ergebnis-Tabellen fehlen weiterhin (§ 11).
- Kein Boot-Smoke-Test gelaufen (`@EnableScheduling`/`@Scheduled` mit `PT5M`); es gibt kein
  `@SpringBootTest` im Repo.

**Reflexion:** Die Doku beschrieb einen Zustand, den der Code nicht kannte. Erst das Schreiben des
Störfalls im ADR hat gezeigt, dass der Zustand **vor** der Promotion gespeichert werden muss und
dass ein Restfenster vor diesem Speichern bleibt. Das ADR nennt dieses Fenster deshalb offen.

---

## 2026-11-05 — ADR-015: OFFEN-Punkte entschieden und umgesetzt (Block 3)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus), Branch `feat/coding-agent`.

**Auftrag (Prompt-Zusammenfassung):** Antworten des Menschen auf die 10 Prüfpunkte des vorherigen
Eintrags: (1) ADR-015 übernehmen, (2) Inhalt bestätigt, (3) DMS-Write nicht idempotent, ein
zweiter Write liefert `400`, (4) Index-Status in den Vertrag, (5) `PT5M` ok, (6) `process_finalization`
ohne Ablauf, Löschung nur auf Antrag je `tenant_id`, (7) Reject in `FINALIZED_INDEX_PENDING`
abweisen, (8) „PENDING prüfen" und „Blobs löschen" aus § 6.2 implementieren, (9) TTL-Cleanup für die
übrigen Tabellen nach sauberster Lösung implementieren, (10) wenn einfach: Test für die
Scheduler-Verdrahtung.

**Geänderte Dateien:**

- neu: `shared/cleanup/TtlPurger.java`, `shared/cleanup/TtlCleanupJob.java`,
  `shared/config/SchedulingConfig.java`, `validation/application/TtlCleanupService.java`,
  `validation/application/{ProcessNotFoundException,ProcessExpiredException,AlreadyFinalizedException}.java`,
  `validation/domain/PendingEmbeddingCheck.java`, `validation/port/PendingEmbeddingCheckPort.java`,
  `validation/adapter/out/postgres/PgPendingEmbeddingCheckAdapter.java`
- gelöscht (aus dem vorherigen Eintrag, ersetzt): `CleanupExpiredPendingEmbeddings(.java|Service.java|ServiceTest.java)`
- geändert: `ConfirmAndWriteBackService` (Vorprüfung Tenant/Ablauf, Blobs löschen, `indexStatus`;
  ungenutzten `AppConfigPort`-Parameter entfernt), `RejectSuggestionService` (Tenant-Prüfung, `409`
  nach DMS-Write, Blobs löschen), `FinalizationRecoveryScheduler` (nur noch Retry),
  `Finalization`/`ConfirmationResult` (+`tenantId`/`indexStatus`), `FinalizationStatePort` +
  `PgFinalizationStateAdapter` (+`deleteByTenant`), `V14__process_finalization.sql` (+`tenant_id`,
  in place, weil noch nicht committet), `ProcessDocumentPort.StoredDocument` +
  `PgProcessDocumentAdapter` (+`tenantId`), `DocumentBlobPort` + `PgDocumentBlobAdapter`
  (`deleteByProcess`, `findExpiredProcessIds`, `deleteExpired`), `PgRetrievalResultAdapter`,
  `PgDocumentTypeCandidatesAdapter`, `PgExtractionResultAdapter` (je `TtlPurger`),
  `GlobalExceptionHandler` (`404`/`410`/`409`), `application.yml` (`docextract.cleanup.interval: PT5M`)
- Tests: `ConfirmAndWriteBackServiceTest` (7 → 12), neu `RejectSuggestionServiceTest` (3),
  `TtlCleanupServiceTest` (2), `SchedulingWiringTest` (2); erweitert
  `PgPendingEmbeddingDeleteAdapterTest` (3 → 5), `PgDocumentBlobAdapterTest` (6 → 8)
- Frontend: `validation.model.ts` (`indexStatus`), `mock-data.ts`, `process-page.spec.ts`
- Doku: `ARCHITECTURE.md` (ADR-015 Status/Entscheidung/Preis/Störfall/Prüfung, § 5.2, § 5.4, § 5.5,
  § 6.2, § 11), `Block-3-Prompts.md` (ADR-014 → ADR-015 für den TRANSFER-Entscheid),
  `doc/api/docextract-api-docs.json` (`ConfirmationResult`, Fehlerantworten confirm/reject)

**Übernommen (generiert):**

- TTL-Cleanup: `shared.cleanup.TtlCleanupJob` ruft alle `TtlPurger`-Beans auf; jedes Modul löscht
  nur eigene Tabellen. Das deckt die Platzierung „shared: TtlCleanupJob" aus § 5.5 ab, ohne dass
  `shared` Fachmodule kennt. Einfache Ergebnistabellen löschen direkt per SQL im Adapter;
  Embeddings und Blobs laufen über den `TtlCleanupService` in `validation`, weil sie
  `FINALIZED_INDEX_PENDING` überspringen müssen (ADR-008: „derselbe TTL-Cleanup wie für
  Embeddings").
- Blobs werden nach Promotion-Versuch gelöscht, unabhängig vom Ergebnis: Die Bytes liegen im DMS,
  der Retry braucht nur die IDs. So bleiben Roh-Bytes nicht bis zum erfolgreichen Retry liegen.
- Vorprüfung: Tenant über die ORIGINAL-Blob-Zeile (immer vorhanden) und zusätzlich über das
  `PENDING`-Embedding; fremder Tenant und unbekannter Prozess liefern beide `404` (NfA-4).
- `mvn -o clean verify` grün (inkl. `HexagonalArchitectureTest`), Frontend `ng test` 54/54 grün.

**Korrigiert / eigene Entscheide (bitte gegenlesen):**

- Fehlt das `PENDING`-Embedding (Retrieval übersprungen, § 6.5), bleibt die Freigabe erlaubt. § 6.2
  sagt „vorhanden"; ein hartes Verbot würde Scans ohne Textlayer blockieren.
- Reject wird nicht nur in `FINALIZED_INDEX_PENDING`, sondern auch in `COMPLETED` mit `409`
  abgewiesen (nach dem DMS-Write gibt es nichts mehr abzulehnen). Reject löscht jetzt auch die
  Blobs (§ 6.2 „Ablehnung … Blobs werden gelöscht") und prüft den Tenant.
- Abgelaufenes `PENDING` beim Confirm → `410 Gone`.

**Verworfen / Veto:** Kein Zustand vor dem DMS-Write: Da das DMS einen zweiten Write mit `400`
abweist, entsteht kein Duplikat; das Restrisiko (Dokument fehlt im Korpus) ist im ADR als
akzeptiert festgehalten. Keine UI-Anzeige für `FINALIZED_INDEX_PENDING` (nur Vertrag).

**Zu prüfen durch Mensch:**

- OFFEN: Es gibt noch keinen Löschantrag-Flow (NfA-5). `FinalizationStatePort.deleteByTenant`
  existiert, wird aber noch nirgends aufgerufen.
- OFFEN: Retry ohne Obergrenze/Backoff; ein dauerhaft scheiternder Promote hält Embedding (und bei
  Crash vor dem Blob-Löschen auch Blobs) unbegrenzt fest. Wann gilt ein Prozess als „endgültiger
  Fehler" (C-7)?
- OFFEN: Im Restfenster (Crash zwischen DMS-Antwort und Zustand) meldet ein erneuter Confirm `422`
  „Invalid attributes", obwohl das Dokument im DMS liegt. Soll der `400` dieses Falls erkannt und
  anders gemeldet werden?
- `V14` wurde in place geändert. Falls lokal bereits angewendet: Datenbank neu aufsetzen oder
  `flyway repair`.
- Soll das UI bei `indexStatus = FINALIZED_INDEX_PENDING` einen Hinweis zeigen?

**Reflexion:** Die offenen Punkte als konkrete Ja/Nein-Fragen zu stellen hat sich gelohnt: Zehn
kurze Antworten genügten für eine eindeutige Umsetzung. Die Info „zweiter Write liefert 400" hat
eine geplante Zusatzmechanik (Zustand vor dem Write) überflüssig gemacht.

---

## 2026-11-05 — ADR-015: Restfenster anders melden, TODO Löschantrag, Empfehlung Retry-Grenze (Block 3)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus), Branch `feat/coding-agent`.

**Auftrag (Prompt-Zusammenfassung):** (1) Fehlenden Löschantrag-Flow als TODO markieren,
(2) Empfehlung für eine Retry-Obergrenze, (3) den `400` im Restfenster anders melden als
„ungültige Attribute", (4) keine UI-Anzeige für `FINALIZED_INDEX_PENDING` nötig.

**Geänderte Dateien:**

- neu: `validation/port/DmsWriteFailedException.java`,
  `validation/application/PossiblyAlreadyFinalizedException.java`
- geändert: `FinalizationStatus` (+`DMS_WRITE_STARTED`), `ConfirmAndWriteBackService` (Marker vor
  dem DMS-Write, `rejected(...)`), `DmsWriteAdapter` (Server-/Netzfehler → `DmsWriteFailedException`
  statt `null`), `FinalizationStatePort` + `PgFinalizationStateAdapter` (`discardWriteStarted`,
  Upsert aktualisiert auch die IDs, TODO an `deleteByTenant`), `V14__process_finalization.sql`
  (`dms_document_id` nullable, erneut in place), `GlobalExceptionHandler` (`409`, `502`)
- Tests: `ConfirmAndWriteBackServiceTest` (12 → 15), `PgPendingEmbeddingDeleteAdapterTest` (5 → 6)
- Doku: `ARCHITECTURE.md` (ADR-015 Schritt 1, Störfall, Prüfung; § 11 Empfehlung + TODO),
  `doc/api/docextract-api-docs.json` (confirm `409`/`502`)

**Übernommen (generiert):** Den `400` selbst kann man nicht vom „ungültige Attribute"-`400`
unterscheiden. Darum speichert der Service vor dem DMS-Write den Marker `DMS_WRITE_STARTED`. Eine
saubere Ablehnung verwirft ihn (`422` wie bisher). Bleibt er nach einem Absturz oder Timeout stehen und
lehnt das DMS den nächsten Versuch ab, antwortet der Confirm mit `409` „Dokument möglicherweise
bereits im DMS gespeichert". `mvn -o clean verify` grün.

**Korrigiert:** `DmsWriteAdapter` hat Timeouts/5xx bisher als `null` gemeldet, also als „ungültige
Attribute". Das ist ein unbekannter Ausgang und wird jetzt als `502` gemeldet; der Marker bleibt.

**Empfehlung (nicht umgesetzt):** Retry-Obergrenze zeitbasiert, 24 h ab dem ersten Fehlversuch
(≈ 288 Versuche bei `PT5M`), Alarm ab 1 h; danach `INDEX_FAILED` mit Audit, Schutz aufheben, damit
der TTL-Cleanup Embedding und Blobs löscht. Kein Backoff nötig, der Versuch ist ein billiges
`UPDATE`. Für die Umsetzung braucht `process_finalization` zusätzlich einen Zeitstempel des ersten
Fehlversuchs.

**Zu prüfen durch Mensch:**

- OFFEN: Empfehlung zur Retry-Obergrenze übernehmen (24 h / Alarm 1 h / `INDEX_FAILED`)?
- OFFEN: Ein `2xx` ohne verwertbaren `Location`-Header gilt weiterhin als Ablehnung (`422`, Marker
  verworfen), obwohl das DMS gespeichert hat (bestehendes Verhalten vom 2026-10-24).
- Ein Reject im Zustand `DMS_WRITE_STARTED` wird mit `409` abgewiesen (Ausgang unbekannt).
- TODO: Löschantrag je Mandant (ARCHITECTURE § 11, `FinalizationStatePort.deleteByTenant`).
- `V14` erneut in place geändert (`dms_document_id` nullable): ggf. lokale DB neu aufsetzen.

**Reflexion:** Die Rückmeldung „anders melden" deckte eine zweite Ursache auf: Der Adapter hat
unbekannte Ausgänge (Timeout) bisher selbst als Ablehnung getarnt. Ohne diese Korrektur hätte der
Marker genau im wichtigsten Fall gefehlt.

---

## 2026-11-05 — ADR-015: Retry-Obergrenze umgesetzt (Block 3)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus), Branch `feat/coding-agent`.

**Auftrag (Prompt-Zusammenfassung):** (1) Empfehlung zur Retry-Obergrenze übernehmen, (2) `2xx`
ohne `Location` bleibt Ablehnung, (3) Reject in `DMS_WRITE_STARTED` → `409` bleibt.

**Geänderte Dateien:**

- neu: `V15__process_finalization_first_failed_at.sql` (V14 ist inzwischen committet, daher neue
  Migration), `shared/config/ClockConfig.java`
- geändert: `FinalizationStatus` (+`INDEX_FAILED`), `Finalization` (+`firstFailedAt`, 5-Argument-
  Konstruktor bleibt), `PgFinalizationStateAdapter`, `ConfirmAndWriteBackService` (erster
  Fehlversuch setzt `firstFailedAt`; Retry-Lauf: nach `promotion-retry-limit` → `INDEX_FAILED` +
  Audit `validation.promotion-abandoned`, ab `promotion-alert-after` eine `ERROR`-Logzeile je Lauf),
  `application.yml` (`PT24H`/`PT1H`), Frontend `validation.model.ts`, OpenAPI-Snapshot (Enum)
- Tests: `ConfirmAndWriteBackServiceTest` (15 → 17), `TtlCleanupServiceTest` (2 → 3),
  `PgPendingEmbeddingDeleteAdapterTest` (6 → 7)
- Doku: `ARCHITECTURE.md` (ADR-015 Entscheidung/Preis/Prüfung, § 5.2, § 11)

**Übernommen (generiert):** `INDEX_FAILED` ist bewusst nicht vor dem TTL-Cleanup geschützt; so
greift C-7 „endgültiger Fehler" ohne eigenen Löschpfad. `java.time.Clock` als Bean, damit die
Zeitregel ohne Wartezeiten testbar ist. `mvn -o clean verify` und `ng test` grün.

**Verworfen / Veto:** Keine Micrometer-Metrik: Das Projekt hat keinen Actuator, eine neue
Abhängigkeit nur dafür wäre über den Auftrag hinaus. Der Alarm ist log-basiert (§ 11 vermerkt).

**Zu prüfen durch Mensch:**

- Log-basierter Alarm (`ERROR` alle 5 min, solange überfällig) ausreichend, oder Actuator/Micrometer
  einführen?
- TODO bleibt: Löschantrag je Mandant (§ 11).

**Reflexion:** Die vorab als Empfehlung formulierte Grenze liess sich direkt umsetzen, weil Daten
(`first_failed_at`) und Ablauf (`INDEX_FAILED` → regulärer Cleanup) schon im Vorschlag benannt waren.

---

## 2026-11-05 — § 5.6 Modulschnitt und Verträge (Block 3, P2 / Artefakt 1)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus), Branch `feat/coding-agent`.

**Auftrag (Prompt-Zusammenfassung):** Neuer Abschnitt § 5.6 in `ARCHITECTURE.md`: Mermaid-Flowchart
der Fachmodule mit Port-Kanten aus dem echten Code, Nähte validation→retrieval und
extraction→retrieval (Änderungsgrund, Kopplung, Resilienz), Satz zu ADR-001, ArchUnit-Regel „Zugriff
zwischen Modulen nur über `application`/`port`", Verletzungen auflisten statt unterdrücken.

**Geänderte Dateien:**

- `doc/ARCHITECTURE.md` (neu § 5.6; bestehende Nummern unverändert)
- `src/test/java/.../architecture/HexagonalArchitectureTest.java` (neue Regel
  `other_modules_access_only_application_port_or_published_domain_types` mit expliziter Liste
  `PUBLISHED_DOMAIN_TYPES`)

**Übernommen (generiert):** Diagramm aus `import`-Analyse des Produktivcodes abgeleitet, nicht aus
§ 5.4. Regel zuerst ohne Ausnahmen laufen lassen (rot), Verletzungen aus dem ArchUnit-Report
gesammelt (23 Typen + 3 nur per `catch` referenzierte Exceptions), dann als benannte Liste in den Test
und als Tabelle in § 5.6 übernommen. Die Regel sammelt Verletzungen über alle Module (statt beim
ersten Modul abzubrechen). Neue Cross-Modul-Zugriffe auf nicht gelistete Domain-Typen schlagen fehl.

**Befunde Code vs. Doku (nicht geändert):**

- validation→retrieval ist **keine** Java-Abhängigkeit: Die Promotion-/Delete-/Check-Ports liegen in
  `validation`, ihre Adapter schreiben direkt in die Tabelle `embedding` (Kopplung über das Schema).
  Im Diagramm gestrichelt.
- Die Pipeline (content → retrieval → extraction) orchestriert `ingest.IngestDocumentService`, nicht
  `process` (§ 5.4 nennt `process` „Async-Orchestrierung").
- extraction nutzt zusätzlich `RetrievalResultPort`, `ContentStagingPort`, `ProcessEventPort`,
  `ProcessRepositoryPort` (in § 5.4 nicht als Outbound-Ports genannt).
- `agentgateway.DocExtractMcpTool` ruft `ConfirmAndWriteBack.confirm(...)` auf, d. h. das MCP-Tool
  bietet heute einen schreibenden Pfad an.

**Zu prüfen durch Mensch:**

- OFFEN: 26 Domain-Typen werden modulübergreifend genutzt (Tabelle § 5.6). Als Vertragstypen nach
  `port`/`shared` verschieben oder als Published Language im `domain`-Paket akzeptieren?
- OFFEN: § 5.4 an den Code angleichen (Orchestrierung in `ingest` statt `process`; fehlende
  Outbound-Ports von extraction) oder Code an § 5.4?
- OFFEN: MCP-Tool mit `confirm` (DMS-Write) widerspricht Block-3-Vorgabe P4 „kein Tool für
  confirm" bzw. ist mit C-2/ADR-003 abzugleichen — Klärung in P4/P5.

**Reflexion:** Die ArchUnit-Regel erst bewusst rot laufen zu lassen lieferte die vollständige
Liste (inkl. transitiv genutzter Typen wie `DmsDocumentType`, die nicht importiert werden); eine reine
`import`-Suche hätte diese übersehen.

---

## 2026-11-05 — Domain-Typen verschoben, § 5.4/§ 5.5 an Code angeglichen (Block 3, P2-Nachtrag)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus), Branch `feat/coding-agent`.

**Auftrag (Prompt-Zusammenfassung):** Entscheide zu den OFFEN-Punkten aus § 5.6 umsetzen. (A) Jeden
Typ aus `PUBLISHED_DOMAIN_TYPES` in `port`/`application` des Besitzermoduls verschieben (Signatur
massgebend), `MediaType` → `shared`, die drei Exceptions bleiben bis P7; nur verschieben, separater
Commit. (B) Doku folgt Code: § 5.4/§ 5.5 (ingest steuert Pipeline, process = Fortschritt/SSE,
fehlende Outbound-Ports von extraction), CONTEXT.md § 5 (nur process).

**Verschobene Typen (alt → neu), Commit `b770a92`:**

| alt                                      | neu                                          |
| ---------------------------------------- | -------------------------------------------- |
| `audit.domain.AuditEvent`                | `audit.application.AuditEvent`               |
| `content.domain.ContentCommand`          | `content.application.ContentCommand`         |
| `content.domain.DocumentContent`         | `content.port.DocumentContent`               |
| `content.domain.ExtractedText`           | `content.port.ExtractedText`                 |
| `ingest.domain.BlobRef`                  | `ingest.port.BlobRef`                        |
| `ingest.domain.IngestCommand`            | `ingest.application.IngestCommand`           |
| `ingest.domain.IngestedDocument`         | `ingest.application.IngestedDocument`        |
| `ingest.domain.MediaType`                | `shared.media.MediaType`                     |
| `ingest.domain.PageRange`                | `ingest.port.PageRange`                      |
| `process.domain.ProcessEvent`            | `process.application.ProcessEvent`           |
| `process.domain.ProcessStep`             | `process.application.ProcessStep`            |
| `process.domain.StepStatus`              | `process.application.StepStatus`             |
| `retrieval.domain.DmsDocumentMetadata`   | `retrieval.port.DmsDocumentMetadata`         |
| `retrieval.domain.DmsDocumentType`       | `retrieval.port.DmsDocumentType`             |
| `retrieval.domain.DmsProperty`           | `retrieval.port.DmsProperty`                 |
| `retrieval.domain.DmsPropertyDataType`   | `retrieval.port.DmsPropertyDataType`         |
| `retrieval.domain.DmsPropertyDefinition` | `retrieval.port.DmsPropertyDefinition`       |
| `retrieval.domain.DmsValueList`          | `retrieval.port.DmsValueList`                |
| `retrieval.domain.RetrievalResult`       | `retrieval.port.RetrievalResult`             |
| `security.domain.AuthContext`            | `security.application.AuthContext`           |
| `security.domain.DvelopCredential`       | `security.application.DvelopCredential`      |
| `validation.domain.ConfirmationCommand`  | `validation.application.ConfirmationCommand` |
| `validation.domain.ConfirmationResult`   | `validation.application.ConfirmationResult`  |

**Übernommen (generiert):**

- Zielregel: Steht ein Typ in einer Signatur im `port`-Paket des Besitzermoduls, geht er nach
  `port` (auch wenn er zusätzlich in `application` vorkommt), sonst nach `application`. So entsteht
  keine Abhängigkeit `port → application` innerhalb eines Moduls (z. B. `DocumentContent`,
  `ExtractedText`, `RetrievalResult`).
- Nicht direkt in einer Signatur, aber transitiv über einen verschobenen Typ: `ProcessStep`/
  `StepStatus` (über `ProcessEvent`), `DmsDocumentType`/`DmsProperty`/`DmsPropertyDefinition`/
  `DmsPropertyDataType` (über `DmsDocumentMetadata`) — mit dem tragenden Typ verschoben.
- Ziel für `MediaType`: `shared.media` (Unterpaket nach Thema wie `shared.hash`, `shared.cleanup`).
- Ohne IDE per `git mv` + Umschreiben von `package`/`import`/Javadoc-FQN; fehlende Imports für
  bisher paketgleiche Referenzen ergänzt. Diff geprüft: nur `package`-, `import`- und Javadoc-Zeilen.
  `process.domain` ist danach leer und entfällt.
- `PUBLISHED_DOMAIN_TYPES` auf `ContentException`, `RetrievalException`, `ExtractionException`
  reduziert. § 5.6 (Tabelle, Nahttext extraction → retrieval), § 5.4, § 5.5 und CONTEXT.md § 5
  (process-Zeile) nachgeführt; § 5.5 zusätzlich bei den verschobenen Typen.
- `mvn -o clean verify` grün, `HexagonalArchitectureTest` grün.

**Commits:** Nur die reinen Verschiebungen sind als `b770a92` committet.
`HexagonalArchitectureTest` (enthält auch die neue P2-Regel), `ARCHITECTURE.md`, `CONTEXT.md` und
dieses Tagebuch sind noch nicht committet, weil P2 noch nicht committet war.

**Zu prüfen durch Mensch:**

- OFFEN: Keine Klasse in einem `domain`-Paket nutzt im Code einen verschobenen Typ. Einzige
  Referenz domain → port: Javadoc `{@link}` in `extraction.domain.ExtractedAttribute` auf
  `retrieval.port.DmsProperty`/`DmsPropertyDefinition` (und `LlmResponse`/`LlmCapabilities` auf das
  eigene `extraction.port.LlmPort`). Belassen?
- Zielpaket `shared.media` für `MediaType` bestätigen.
- `security.application.DvelopCredential` wird jetzt aus `port`-Paketen anderer Module importiert
  (`ingest.port`, `retrieval.port`, `validation.port` → `security.application`). Keine ArchUnit-Regel
  verbietet das. Ist es so gewollt?
- Die übrigen OFFEN-Punkte aus dem P2-Eintrag sind erledigt (Domain-Typen, § 5.4). Weiterhin offen
  ist das MCP-Tool mit `confirm` (P4/P5).

**Nachtrag (Entscheide Mensch zu den OFFEN-Punkten oben):**

- Erledigt: `extraction.domain.ExtractedAttribute` — `{@link retrieval.port.DmsPropertyDefinition}`/
  `{@link retrieval.port.DmsProperty}` durch `{@code …}` ersetzt. Kein `domain`-Javadoc verweist mehr
  auf `port`/`application`/`adapter` eines fremden Moduls (geprüft per `grep`).
- Entschieden: `shared.media.MediaType` bleibt. Geprüft: einziger Import `java.util.Locale`, keine
  Framework-Typen.
- Entschieden: `port` → `security.application.DvelopCredential` (aus `ingest.port`,
  `retrieval.port`, `validation.port`) ist gewollt.
- Aufgabe P4: MCP-Tool `confirm` (`DocExtractMcpTool` → `ConfirmAndWriteBack`) entfernen.
- Aufgabe P5: `ConsentGuard` verweigert Kanal MCP für confirm/reject/template.
- `mvn -o clean verify` grün. Commit nur mit `ExtractedAttribute.java`, getrennt von P2; das
  Tagebuch wird mit den P2-Dokumenten committet, weil die Einträge in einem zusammenhängenden,
  noch nicht committeten Block stehen.

---

## 2026-11-05 — OpenAPI-Vertrag mit Fehlerfällen (Block 3, P3 / Artefakt 2)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus), Branch `feat/coding-agent`.

**Auftrag (Prompt-Zusammenfassung):** Versionierte `doc/api/openapi.yaml` (OpenAPI 3.1) für alle
öffentlichen Endpunkte; springdoc vorhanden → generieren + Drift-Check-Test. Fehler als
`application/problem+json` mit gemeinsamem Schema `Problem`, nur tatsächlich gelieferte Codes.
`confirm` vollständig mit `x-idempotency`, `x-data-classification`, `x-slo` (Platzhalter OFFEN).
Idempotenz-Test (zweimal confirm → `DmsWritePort` genau 1×). Verweis in ARCHITECTURE.md § 8.1.

**Geänderte Dateien:**

- `doc/api/openapi.yaml` (neu, generiert)
- `src/main/java/.../shared/config/OpenApiConfig.java` (Server `/adeon-docextract`, Customizer:
  Schema `Problem`, Responses `BadRequest`/`Unauthorized`, Tags sortiert)
- Controller-Annotationen (`@Operation`, `@ApiResponse`, `@Tag`): `UploadController`,
  `PreviewController`, `ImportInfoController`, `ProcessEventsController`, `RetrievalController`,
  `ExtractionController`, `ValidationController`, `HealthController`
- `src/main/resources/application.yml` (`springdoc.paths-to-match` + `/health`,
  `default-produces-media-type`, `override-with-generic-response: false`,
  `writer-with-order-by-keys: true`)
- `pom.xml` (Versionsausrichtung `swagger-annotations-jakarta` 2.2.41, Exclusion
  `swagger-annotations` aus `spring-ai-starter-model-openai`; keine neue Dependency)
- `src/test/java/.../shared/config/OpenApiContractTest.java` (neu)
- `src/test/java/.../validation/adapter/in/rest/ValidationControllerIdempotencyTest.java` (neu)
- `doc/ARCHITECTURE.md` (§ 8.1, nur ergänzt)

**Übernommen (generiert):**

- Vertrag aus Annotationen in den Adaptern (`adapter.in.rest`, `shared`), nicht von Hand: Die Datei
  ist exakt die springdoc-Ausgabe. `OpenApiContractTest` startet nur Web MVC + springdoc mit allen
  per Classpath-Scan gefundenen `@RestController`s (Abhängigkeiten gemockt, `application.yml`
  geladen) und vergleicht mit der committeten Datei. Neue Controller fallen damit automatisch in
  den Drift-Check. Zusätzlich Parser-Test (swagger-core `Yaml31`): `openapi: 3.1.x`, alle `$ref`
  auflösbar, jede 4xx/5xx-Antwort mit `application/problem+json`, jede `/api/`-Operation mit `401`,
  `confirm` mit den drei x-Extensions und den Codes 200/400/401/404/409/410/422/502.
- Gegenprobe: App im Profil `ci` gestartet, `/v3/api-docs.yaml` live abgerufen — identisch mit
  `doc/api/openapi.yaml`.
- Fehlercodes je Endpunkt aus `GlobalExceptionHandler`, `SecurityConfig` und den geworfenen
  Exceptions der Services abgeleitet. Idempotenzregel aus `ConfirmAndWriteBackService`: Wiederholung
  nach dem DMS-Write (`FINALIZED_INDEX_PENDING`/`COMPLETED`/`INDEX_FAILED`) → `200` mit gespeicherten
  IDs, kein zweiter DMS-Write; bei `FINALIZED_INDEX_PENDING` nur Promotion; bei `DMS_WRITE_STARTED`
  erneuter Write, DMS-Ablehnung → `409`.
- `ValidationControllerIdempotencyTest`: MockMvc + echter `ConfirmAndWriteBackService` mit
  zustandsbehaftetem In-Memory-`FinalizationStatePort`; (1) zweimal confirm → gleiche Antwort,
  `DmsWritePort` 1×, Promotion 1×; (2) Promotion-Fehler beim ersten Aufruf → zweiter Aufruf
  `COMPLETED`, `DmsWritePort` 1×, Promotion 2×.
- `mvn -o clean verify` grün (inkl. ArchUnit).

**Korrigiert:**

- Erster Generierungslauf scheiterte mit `NoSuchMethodError Schema.$dynamicRef()`: Spring AI zieht
  `swagger-annotations-jakarta` 2.2.38 (gewinnt gegen springdocs 2.2.41) und `openai-java-core` die
  nicht-jakarta `swagger-annotations` 2.2.31 (gleiche Pakete, liegt im Classpath vorne). Latenter
  Fehler: `/v3/api-docs` wäre auch live gebrochen, sobald ein Controller `@Schema` trägt. Behoben per
  Versionsausrichtung + Exclusion (keine neue Dependency).

**Befunde (nicht geändert):**

- Pfad `confirm`/`reject` im Code: `POST /api/v1/validation/confirm|reject` mit `processId` im Body;
  § 6.2 zeigt `POST /documents/{id}/confirm`.
- `403`: `SecurityConfig` hat einen `accessDeniedHandler`, aber alle Regeln sind nur
  `authenticated()` → nicht erreichbar, daher nicht dokumentiert.
- `503`/`504`: werden nicht geliefert; Upstream-Fehler mit Mapping liefern `502`.
- Framework-Fehler (fehlender Parameter, ungültige UUID, unlesbares JSON → `400`; Multipart-Limit →
  `413`) liefern heute nur den Status ohne Body (`SpaFallbackErrorController`). Im Vertrag als
  `Problem` mit Hinweis „ProblemDetail ab P7".
- Ohne Mapping → `500` ohne Body: `DmsRepositoryException` (import-info), `DmsObjectDefinitionException`
  (object-definitions), `ValueListWebhookException` (value-lists), `ExtractionException` (retry),
  ungültiger `Range`-Header (`NumberFormatException`/`IllegalArgumentException` in
  `PreviewController.parseRange`). Nicht dokumentiert.
- Kein Ownership-Check (NfA-4) bei `GET /processes/{id}/events`, `/similar-documents`,
  `/document-type-candidates`, `/extraction-result` und `POST .../extraction-result/retry`:
  liefern für fremde/unbekannte `processId` `200` (leer bzw. Daten, sofern vorhanden).
- Limit-Verstoss in `Limits` liefert `400`, § 6.5 nennt `413`/`422`.

**Zu prüfen durch Mensch:**

- OFFEN: `x-slo` für `confirm` — Zielwert p95 und Verfügbarkeit festlegen.
- OFFEN: § 6.2 an den Code angleichen (`/api/v1/validation/confirm`, `processId` im Body) oder API
  auf `/documents/{id}/confirm` umstellen?
- OFFEN: `doc/api/docextract-api-docs.json` + Maven-Profil `openapi-spec` (CONTEXT.md, README,
  FRONTEND-SPEC, frontend/README verweisen darauf) durch `openapi.yaml` ablösen oder parallel
  behalten? Heute zwei Vertragsdateien, nur `openapi.yaml` ist drift-geprüft.
- OFFEN: Ownership-Check für die Lese-Endpunkte und SSE (siehe Befunde) — Teil von P5?
- OFFEN: Unmapped Exceptions (`500` ohne Body) und Framework-`400`/`413` in P7 auf ProblemDetail
  abbilden; Limit-Verstoss `400` vs. `413`/`422` (§ 6.5) entscheiden; danach `openapi.yaml`
  regenerieren.
- OFFEN: Diagnose-Endpunkt `GET /api/v1/ingest/documents/test` entfernen? (öffentlich erreichbar,
  daher im Vertrag.)
- Prüfen: Exclusion `swagger-annotations` aus `spring-ai-starter-model-openai` (App startet, Tests
  grün; LLM-Pfad gegen echten Anbieter nicht getestet).
- Wiederholter `confirm` wertet geänderte `attributes` nicht aus (stillschweigend ignoriert) — so
  gewollt oder `409` bei abweichendem Body?

**Reflexion:** Den Vertrag aus Annotationen zu generieren statt die YAML zu pflegen, macht den
Drift-Check trivial und hat nebenbei einen latenten Classpath-Konflikt aufgedeckt, der live erst
bei der ersten `@Schema`-Annotation aufgefallen wäre.

---

## 2026-11-05 — P3-Nachtrag: SLO, Pfade, Vertrag konsolidiert (Block 3, P3)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus), Branch `feat/coding-agent`.

**Auftrag (Prompt-Zusammenfassung):** Entscheide des Menschen zu den P3-OFFEN-Punkten umsetzen:
`x-slo` mit Werten füllen; Pfade in ARCHITECTURE.md (§ 3.1, § 6.1, § 6.2, ADR-015) an
`openapi.yaml` angleichen (Doku folgt Code); `docextract-api-docs.json` + Profil `openapi-spec`
entfernen und Verweise umstellen; `GET /api/v1/ingest/documents/test` entfernen;
Body-Regel bei Wiederholung in `x-idempotency`; Folgeaufgaben für P5/P7 in `Block-3-Prompts.md`.

**Geänderte Dateien:**

- `ValidationController.java` (`x-slo`: `latency` p95/2s/30d, `availability` 99.5 %/30d,
  `measurement`; `x-idempotency.request-body-on-repeat`)
- `UploadController.java` (Endpunkt `/test` und ungenutzter `AuthContextPort` entfernt),
  `e2e/ingest.http` (Request „Test Authentication" entfernt)
- `OpenApiContractTest.java` (prüft SLO-Felder statt „OFFEN", Body-Regel vorhanden)
- `doc/api/openapi.yaml` (neu generiert), `doc/api/docextract-api-docs.json` (gelöscht)
- `pom.xml` (Profil `openapi-spec` samt `springdoc-openapi-maven-plugin` entfernt)
- `doc/CONTEXT.md`, `README.md`, `doc/frontend/FRONTEND-SPEC.md`, `frontend/README.md`
  (Verweise → `doc/api/openapi.yaml`, Regenerierung per `OpenApiContractTest`)
- `doc/ARCHITECTURE.md` (§ 3.1, § 6.1, § 6.2, ADR-015)
- `doc/Block-3-Prompts.md` (P5: Ownership-Check Lese-Endpunkte/SSE; P7: Fehlerabbildungen, 413/422,
  Range/416)

**Übernommen (generiert):**

- `x-slo`-Werte unverändert aus der Vorgabe des Menschen übernommen (verschachtelt per
  `@ExtensionProperty(parseValue = true)`).
- Frontend: keine Client-Generierung aus der JSON-Datei (kein `package.json`-Skript), CI
  (`.github/workflows/ci.yml`) referenziert sie nicht — nur Doku-Verweise umgestellt, kein
  Frontend-Build nötig. Frontend-Code ruft `/test` nicht auf.
- `mvn -o clean verify` grün.

**Abweichungen Doku ↔ `openapi.yaml` (angeglichen):**

| Stelle        | vorher                                                                | jetzt (Code)                                                                                                                     |
| ------------- | --------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- |
| § 3.1 SSE     | `GET /processes/{id}/events`                                          | `GET /api/v1/processes/{processId}/events`                                                                                       |
| § 3.1 Events  | `preview_ready`, `text_extracted`, `retrieved`, `extracted`, `failed` | Event-Name = Schritt `blob_stored`, `dms_uploaded`, `preview`, `text_extracted`, `retrieved`, `extracted`; `FAILED` als `status` |
| § 6.1 Upload  | `POST /documents`                                                     | `POST /api/v1/ingest/documents` (multipart `file`, `repositoryId`)                                                               |
| § 6.1 SSE     | „SSE abonnieren", `preview_ready`                                     | `GET /api/v1/processes/{processId}/events`, `preview`                                                                            |
| § 6.2 Confirm | `POST /documents/{id}/confirm`                                        | `POST /api/v1/validation/confirm` (`processId` im Body)                                                                          |
| § 6.2 Reject  | ohne Pfad                                                             | `POST /api/v1/validation/reject` (`processId` im Body)                                                                           |
| ADR-015       | „Confirm-Request" ohne Pfad                                           | `POST /api/v1/validation/confirm`                                                                                                |

Die Event-Namen sind keine Pfade, standen aber in derselben Tabellenzelle und wichen vom Code
(`ProcessStep`, `ProcessEventsController`) ab; deshalb mit angeglichen.

**Zu prüfen durch Mensch:**

- `x-slo`: „Metriken ab Block 5.3" — bis dahin ist das SLO nur über Logs messbar (keine
  Micrometer/Actuator-Dependency im Repo).
- § 3.1/§ 6.1: Angleichung der SSE-Event-Namen bestätigen (über „Pfadangaben" hinaus).
- Weiterhin offen aus P3, jetzt als Aufgaben in `Block-3-Prompts.md` eingetragen: Ownership-Check
  Lese-Endpunkte/SSE (P5); Fehlerabbildungen, 413/422, Range/416 (P7).
- Verweise in älteren Tagebucheinträgen auf `docextract-api-docs.json`/`openapi-spec` bewusst nicht
  geändert (historisch).

---

## 2026-11-06 — MCP-Tool für Agenten (Block 3, P4 / Artefakt 3)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus).

**Auftrag (Prompt-Zusammenfassung):** Modul `agentgateway` (`adapter.in.mcp`) mit lauffähigem
MCP-Tool: Spring AI MCP Server nur im Adapter (ADR-009); nur lesende Tools auf denselben
Application-Services wie REST (`get_process_suggestions`, `find_similar_documents`); kein Tool für
confirm/DMS-Write (C-2); JSON-Schema mit strikter Validierung; Agent-Identität über bestehende
Lösung oder mandantengebundenen Service-Token (OFFEN); per `docextract.adapters.mcp.enabled`
abschaltbar (Default `false`). Tests: fremder Mandant, ungültige Eingabe, Audit je Call, ArchUnit.
Doku: ARCHITECTURE.md § 6.3, README „MCP-Tool lokal testen".

**Geänderte Dateien:**

- `pom.xml` (`spring-ai-starter-mcp-server-webmvc`, Version über `spring-ai-bom` 2.0.0)
- `src/main/resources/application.yml` (`docextract.adapters.mcp.enabled: false`,
  `spring.ai.mcp.server.*`: an die Property gekoppelt, Streamable HTTP, Endpunkt `/api/v1/mcp`,
  nur Tool-Capability)
- `agentgateway/application/`: `McpTool` (neu geschnitten), `AgentGatewayService`,
  `ProcessSuggestions`, `SimilarDocument`, `AgentToolException` (neu)
- `agentgateway/adapter/in/mcp/`: `McpToolAdapter`, `McpServerConfig` (neu);
  `DocExtractMcpTool` + `DocExtractMcpToolTest` (gelöscht)
- `extraction/domain/ExtractionResult`, `ExtractedAttribute` → `extraction/application/` (Imports in
  `extraction` angepasst)
- `security/adapter/in/web/SecurityConfig.java` (`DispatcherType.ASYNC` → `permitAll`)
- Tests: `AgentGatewayServiceTest`, `McpToolAdapterTest`, `McpServerWiringTest` (neu);
  `HexagonalArchitectureTest` (Regel `spring_ai_and_mcp_types_stay_in_adapters`)
- `doc/ARCHITECTURE.md` (§ 5.4 agentgateway-Zeile, § 5.5 extraction/agentgateway, § 5.6
  Diagramm-Kanten + ArchUnit-Regel, § 6.3 Toolliste/Transport/AuthN), `README.md`

**Übernommen (generiert):**

- Bestand: `DocExtractMcpTool` war ein nicht verdrahteter Stub mit `ingest` und `confirm` (ohne
  Attribute). `confirm` über MCP widerspricht C-2/ADR-003, `ingest` ist schreibend (DMS-Chunk-Upload)
  → beide entfernt, `McpTool` bietet nur noch die zwei Lese-Operationen.
- `AgentGatewayService` (application) ruft `GetExtractionResult`/`GetSimilarDocuments` — dieselben
  Services wie `ExtractionController`/`RetrievalController`. Davor: Format-Check `processId`
  (kleingeschriebene UUID, gleiches Pattern wie im Tool-Schema), Ownership über
  `ProcessDocumentPort.find(...).tenantId()` gegen `AuthContextPort` (fremd und unbekannt → beide
  `not_found`), danach im `finally` ein `AuditEvent` je Aufruf (`mcp.tool.<name>.<outcome>`,
  SHA-256 der `processId`, wie `ValueListLookupTool`).
- `find_similar_documents` gibt nur `documentId`, `repositoryId`, `score` aus (nicht die
  DMS-Metadaten aus `RetrievalResult.properties`), `get_process_suggestions` ohne `sourceExcerpt`
  (Datenminimierung, C-3).
- `ExtractionResult`/`ExtractedAttribute` lagen in `extraction.domain`; der Zugriff aus
  `agentgateway` hätte `other_modules_access_only_application_port_or_published_domain_types`
  verletzt. Gemäss bestehender Konvention (Vertragstyp im `application`-Paket des Besitzers, siehe
  Eintrag „Domain-Typen verschoben") verschoben statt in die Ausnahmeliste aufgenommen.
- Agent-Identität: **bestehende Lösung gefunden** — `SecurityConfig` sichert `/api/**` mit
  `DvelopAuthenticationFilter` (Bearer-Token oder `AuthSessionId`, Prüfung beim d.velop IdP). Der
  MCP-Endpunkt liegt deshalb unter `/api/v1/mcp`; kein eigener Service-Token eingeführt.
- Spring AI 2.0 registriert für Servlet-Apps einen Customizer mit `immediateExecution(true)`
  (Tool-Call auf dem Request-Thread → `SecurityContext` sichtbar). Das MCP-SDK validiert
  Tool-Argumente zusätzlich selbst gegen das Schema und lehnt ungültige Aufrufe ab, **bevor**
  DocExtract sie sieht → kein Audit-Eintrag. Daher eigener `@Primary`-Customizer:
  `immediateExecution(true)` + `validateToolInputs(false)`; die Prüfung im Service ist gleich streng.
- Live-Smoke-Test (`dev`-Profil, dvelop-mock, Postgres, MCP an): ohne Token `401`; `initialize`,
  `tools/list` (2 Tools mit Schema/Annotations), `tools/call` mit eigenem Prozess → Ergebnis, mit
  unbekannter UUID → `not_found`, mit `"x"` → `invalid_argument`.
- Dabei gefunden: Nach jeder SSE-Antwort (MCP, vermutlich auch `/processes/{id}/events`) loggte
  Spring Security beim `ASYNC`-Redispatch `AuthorizationDeniedException` als ERROR (Antwort war
  schon geschrieben; stateless → kein Kontext im Redispatch). Fix: `ASYNC`-Dispatch `permitAll`
  (Standardempfehlung von Spring Security); der ursprüngliche Request bleibt authentisiert.
  Nachher 0 ERROR im Log.
- `mvn -o clean verify` grün (125 Tests), ArchUnit grün.

**Zu prüfen durch Mensch:**

- OFFEN: Mandant des Agenten. `SessionAuthContextAdapter` liefert für jeden Aufrufer fest
  `default-tenant` (gilt auch für REST); der Fremd-Mandanten-Test belegt die Prüfung nur mit
  gemocktem `AuthContextPort`. Wie wird `tenant_id` aus der d.velop-Identität abgeleitet, und
  erhält der Agent einen eigenen technischen d.velop-Benutzer/API-Key (statt eines neuen
  DocExtract-Service-Tokens)? Begründung für „kein eigener Token": eine zweite
  Authentisierungsart wäre ein neuer, ungeprüfter Sicherheitspfad; die bestehende IdP-Prüfung gilt
  für REST und MCP gleich.
- OFFEN: Scopes (C-3/T-6). Der Agent hat heute dieselben Rechte wie ein Benutzer auf `/api/**`
  (z. B. auch REST-`confirm`, wenn er den REST-Pfad direkt ruft). Scope-Liste und Sperre des
  Kanals MCP im `ConsentGuard` → P5.
- OFFEN: Modell-ID und lokal/extern je Vorschlag. `ExtractionResult` persistiert `ModelInfo` nicht
  (`extraction_result` hat nur `document_type_id`, `attributes`); das Tool gibt sie deshalb nicht
  aus statt die aktuelle Konfiguration als Herkunft auszugeben. Persistieren (neue Migration) oder
  weglassen?
- Bestätigen: `sourceExcerpt` und DMS-Metadaten bewusst nicht an den Agenten.
- Audit: `AuditPort` ist weiterhin `NoopAuditAdapter`, `AuditEvent` hat keine Felder für Mandant,
  Kanal, Akteur, Tool → Einträge werden erzeugt (Tests), aber nicht gespeichert. Datenmodell → P5.
- Ownership-Check liegt vorerst in `AgentGatewayService`, nicht zentral; REST-Lese-Endpunkte haben
  ihn noch nicht. Er stützt sich auf die ORIGINAL-Zeile in `document_blob` (mit `dms_location`):
  nach Freigabe/Ablehnung/TTL ist der Prozess für den Agenten `not_found`. Zentraler
  `ProcessAccessGuard` für REST + MCP → P5.
- Guardrail-Code geändert: `SecurityConfig` (`ASYNC` → `permitAll`) — bitte gegenlesen.
- `@Primary`-Customizer ersetzt den Servlet-Customizer von Spring AI; bei Spring-AI-Upgrades prüfen,
  ob dieser weitere Einstellungen setzt.
- Tool-Fehlerformat (`code`, `message`) ist noch nicht mit REST-ProblemDetail vereinheitlicht → P7.
- „Top-5" hängt an `docextract.retrieval.top-k: 5` (und `min-score`), nicht am Tool.

---

## 2026-11-06 — P4-Nachtrag: Entscheide zu den OFFEN-Punkten (Block 3, P4)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus).

**Auftrag (Prompt-Zusammenfassung):** Entscheide des Menschen zu den P4-OFFEN-Punkten
dokumentieren (Mandant, Agent-Identität, Modell-ID, Datenminimierung); Upgrade-Hinweis am
`@Primary`-Customizer; Tests für „ungültiger MCP-Aufruf wird auditiert" und „ohne Token `401` auf
`/api/v1/mcp` und `/processes/{id}/events`"; Freigabe nur für `DispatcherType.ASYNC`.

**Geänderte Dateien:**

- `McpServerConfig.java` (Javadoc: bei Spring-AI-Upgrades prüfen)
- `McpEndpointSecurityTest.java` (neu)
- `doc/ARCHITECTURE.md` § 6.3, `README.md` („MCP-Tool lokal testen")

**Entscheide (Mensch) und Umsetzung:**

1. **Mandant — geschlossen.** `default-tenant` bleibt vorerst; der Mandant kommt nach d.velop-Standard
   bei der Authentisierung aus den HTTP-Headern. Kein Code geändert. **Annahme:** Der
   Mandanten-Header gilt nur, wenn ihn der d.velop-Proxy bzw. IdP gesetzt hat. Ein Client, der
   DocExtract direkt anspricht, darf ihn nicht selbst setzen können. Voraussetzung dafür: DocExtract
   ist nur über den Proxy erreichbar, oder der Mandant wird aus der IdP-Validierung abgeleitet.
2. **Agent-Identität — technischer d.velop-User.** Der Agent erhält einen technischen d.velop-User
   mit API-Key als Bearer-Token und läuft über den bestehenden `DvelopAuthenticationFilter`; kein
   eigener Token-Mechanismus. In § 6.3 und README dokumentiert. **OFFEN (P5), Vorschlag, nicht
   umgesetzt:**
   - konfigurierte Liste der Agent-User, z. B. `docextract.agent.principals`;
   - für diese User hat jeder Aufruf den Kanal MCP;
   - der `ConsentGuard` lehnt für sie `confirm`, `reject` und `template` auch über REST ab.
3. **Modell-ID und lokal/extern — weggelassen.** Keine Migration, das Tool gibt beides nicht aus.
   Begründung: Die Konfiguration zeigt nur das aktuelle Modell; welches Modell ein gespeichertes
   Ergebnis erzeugt hat, wird nicht gespeichert — wegen der kurzen TTL (`result-ttl` PT2H)
   vertretbar. Für C-4/NfA-7 zählt das Audit (Modell-ID je Lauf), nicht die Ausgabe des Tools.
   **OFFEN:** Für FR-5 (Modellinfo im Validierungs-UI) besteht dieselbe Unschärfe.
4. **Datenminimierung — bestätigt.** An den Agenten gehen weder `sourceExcerpt` noch die
   Eigenschaftswerte der ähnlichen Dokumente (`RetrievalResult.properties`). Von ähnlichen
   Dokumenten erhält er nur `documentId`, `repositoryId` und `score`. Die Attributvorschläge
   (`value`/`values`, `confidence`) des eigenen Dokuments gehen an ihn und können Personendaten
   enthalten. Begründung: Die Eigenschaftswerte wurden mit der Session des Benutzers geladen; gibt
   DocExtract sie weiter, kann der Agent Werte sehen, für die er im DMS keine Rechte hat. Über die
   `documentId` kann er sie selbst im DMS abfragen, dann gelten seine eigenen Rechte.
   **Restrisiko:** Das LLM sieht die Werte der Top-5 als Kontext; ein Vorschlag kann deshalb einen
   Wert aus einem ähnlichen Dokument übernehmen.

**Abweichungen:**

- SDK-Validierung abgeschaltet: akzeptiert. Upgrade-Hinweis am `@Primary`-Customizer in Code und
  § 6.3.
- Neuer `McpEndpointSecurityTest` (echtes Tomcat + `SecurityConfig` + Spring AI MCP Server, ohne DB):
  ohne Token `401` auf `POST /api/v1/mcp` und `GET /api/v1/processes/{id}/events`; ein
  schema-ungültiger `tools/call` (`processId: "x"`) erreicht `AgentGatewayService` und erzeugt den
  Audit-Aufruf `mcp.tool.get_process_suggestions.invalid_argument`. Gegenprobe: mit
  `validateToolInputs(true)` schlägt der Test fehl (vorher nur Unit-Tests direkt am Handler, die die
  SDK-Validierung nicht abdecken).
- `SecurityConfig`: Freigabe ist auf `dispatcherTypeMatchers(DispatcherType.ASYNC)` beschränkt, keine
  Pfad-Freigabe; Gegenlesen durch Mensch steht aus.
- Testdetail: Properties per `SpringApplicationBuilder.properties(...)` sind Default-Properties und
  verlieren gegen `application.yml` — im Test daher als Command-Line-Argumente gesetzt.

**Unverändert offen:** Audit-Persistenz und Felder in `AuditEvent` (P5), zentraler Ownership-Check
(P5), Scopes/Agent-Principals (P5), Fehlerformat ProblemDetail (P7), Modellinfo im UI (FR-5).

---

## 2026-11-08 — Mandant in allen Port-Aufrufen statt Guard (Block 3, P5 / Artefakt 4, Teil)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus).

**Auftrag (Prompt-Zusammenfassung):** P5 „Sicherheitskonzept Agent / MCP". Ein erster Entwurf mit
`ProcessAccessGuard`, `ConsentGuard`, Kanal/Scopes und Audit-Tabelle wurde auf Rückfrage
**vollständig zurückgenommen** (Stash `P5-attempt: ProcessAccessGuard/ConsentGuard/audit_entry`,
nicht gelöscht) und durch einen einfacheren Ansatz ersetzt: die `tenantId` geht in **alle**
prozessbezogenen Port-Aufrufe, die Tabellen filtern danach, keine Guards. Mandant = Wert aus
`application.yml`, im Cloud-Betrieb ein Request-Header, im `DvelopAuthenticationFilter` in den
Kontext gesetzt.

**Kritik am verworfenen Entwurf (Grund für den Wechsel):** Der Guard holte den Mandanten aus dem
ThreadLocal-Kontext (in asynchronen Threads falsch), war ein zweiter, vergessbarer Schritt vor dem
Lesen, kostete eine Extra-Abfrage je Aufruf, und die Ports blieben ungefiltert. Zusätzlich lag in
`FindSimilarService`/`StagePendingEmbeddingsService` ein bestehender Fehler: sie riefen
`AuthContextPort.current()` im asynchronen Pipeline-Thread auf; das fiel nur nicht auf, weil der
Mandant konstant `default-tenant` war.

**Geänderte Dateien:**

- Mandant: `application.yml` (`docextract.tenant.default-id`, `docextract.tenant.header`),
  `security/domain/RequestTenant` (neu), `DvelopAuthenticationFilter` (Header lesen und prüfen),
  `SecurityConfig`, `SessionAuthContextAdapter` (Header, sonst Default)
- `V16__tenant_on_process_tables.sql` (neu): `tenant_id` in `process_step`, `retrieval_result`,
  `document_type_candidate`, `extraction_result` (Bestand mit `default-tenant` aufgefüllt)
- Ports mit `tenantId` als erstem Parameter: `ProcessEventPort`, `ProcessDocumentPort` (`StoredDocument`
  ohne `tenantId`), `ProcessRepositoryPort`, `RetrievalResultPort`, `DocumentTypeCandidatesPort`,
  `ExtractionResultPort`, `DocumentBlobPort` (`describe`, `readRange`, `delete`, `deleteByProcess`),
  `FinalizationStatePort` (`find`, `discardWriteStarted`), `PendingEmbeddingCheckPort`
  (`PendingEmbeddingCheck` ohne `tenantId`), `CorpusPromotionPort`, `PendingEmbeddingDeletePort.delete`;
  die Postgres-Adapter filtern mit `AND tenant_id = ?`
- Services: `FindSimilar`, `StageEmbeddings`, `ExtractAttributes`, `RetryExtraction`, `GetSimilarDocuments`,
  `GetDocumentTypeCandidates`, `GetExtractionResult` mit `tenantId`-Parameter;
  `FindSimilarService`/`StagePendingEmbeddingsService` ohne `AuthContextPort`;
  `IngestDocumentService`, `PreviewGenerationService`, `ExtractTextService`, `ContentService`,
  `ExtractAttributesService`, `PreviewStreamService`, `ConfirmAndWriteBackService`,
  `RejectSuggestionService` (stark vereinfacht), `TtlCleanupService`, `AgentGatewayService`
- Controller: `ProcessEventsController`, `RetrievalController`, `ExtractionController`
  (lösen den Mandanten auf), `ValidationController` (OpenAPI: `404` bei `reject` entfällt)
- Tests neu: `TenantResolutionTest`, `TenantScopedEndpointsTest`, `PgTenantFilterAdapterTest`;
  erweitert/angepasst: `PgProcessEventAdapterTest`, `PgProcessRepositoryAdapterTest`,
  `PgRetrievalResultAdapterTest`, `PgDocumentBlobAdapterTest`, `PgPendingEmbeddingDeleteAdapterTest`,
  `ConfirmAndWriteBackServiceTest`, `RejectSuggestionServiceTest`, `FindSimilarServiceTest`,
  `StagePendingEmbeddingsServiceTest`, `PreviewStreamServiceTest`, `AgentGatewayServiceTest`,
  `McpToolAdapterTest`, `McpEndpointSecurityTest`, `TtlCleanupServiceTest`,
  `ValidationControllerIdempotencyTest`, `IngestDocumentServiceTest`
- `doc/ARCHITECTURE.md` (§ 0, § 5.2, § 6.3, § 8.2, § 8.4, neu § 8.6, ADR-015-Testnamen),
  `doc/api/openapi.yaml` (regeneriert)

**Übernommen (generiert):**

- Header-Name `x-dv-tenant-id`: nur als Beispiel/Doku, nicht als Default. Die Property ist leer,
  dann wird ein Header ignoriert (kein Mandantenwechsel durch den Client bei direktem Zugriff).
  Der Wert wird geprüft (`[A-Za-z0-9._-]{1,128}`), sonst `401`.
- Request-Thread: Controller/Adapter lösen den Mandanten auf und geben ihn explizit weiter;
  Pipeline-Schritte bekommen ihn als Parameter (wie das Credential). Der Promotion-Retry nutzt
  `Finalization.tenantId`.
- Systemjobs bleiben mandantenübergreifend (`findExpiredProcessIds`, `deleteExpired`,
  `findByStatus`); `TtlCleanupService` holt die geschützten `FINALIZED_INDEX_PENDING`-Prozesse einmal
  per `findByStatus` statt je Prozess.
- `RejectSuggestionService`: die manuelle Prüfung „fremder Mandant" entfällt, die Ports sind
  gefiltert. Folge: ein unbekannter oder fremder Prozess ergibt `204` ohne Wirkung (vorher `404` bei
  fremd).
- Fremder Mandant bei Lese-Endpunkten: `200` mit leerem Ergebnis statt `404` (aus der Abfrage lässt
  sich „fremd" nicht von „noch kein Ergebnis" unterscheiden, ohne eine zusätzliche Besitzer-Abfrage).
  MCP behält `not_found` über `ProcessDocumentPort.find(tenantId, processId)`.
- Live-Smoke (`dev`, Header aktiv, Port 18080): V16 migriert, Start ohne Fehler; Header `tenant-a` und
  `tenant-b` ergeben `200 []`, ungültiger Header `401`.
- `mvn -o clean verify` grün (156 Tests, ArchUnit grün).

**Verworfen:**

- `ProcessAccessGuard`, `ConsentGuard`, `Channel`/`Scope`, `audit_entry`/`PgAuditAdapter`: im Stash,
  nicht übernommen.

**Zu prüfen durch Mensch:**

- OFFEN: Die P5-Punkte „minimale Rechte (Scopes)", „menschliche Freigabe für Kanal MCP
  (`ConsentGuard`)" und „Audit je Tool-Call (Tabelle, `run_id` nullable, `channel`, `actor_ref`,
  `tool_name`)" sind **nicht umgesetzt** (siehe ARCHITECTURE.md § 8.6, „Offen"). Ohne Kanal im Kontext
  kann ein Agent-Key auch die REST-Endpunkte aufrufen; abgefedert nur durch den User ohne DMS-Rechte.
  Umsetzen, und wenn ja wie (Entwurf im Stash `git stash list`)?
- OFFEN: Header-Name und Vertrauen: `x-dv-tenant-id` ist aus `dvelop-dmsapp.yaml` abgeleitet (dort
  als Mandanten-Header für Rate-Limits genannt), nicht gegen die d.velop-Dokumentation für Apps
  geprüft. Muss der Proxy den Header setzen/überschreiben, oder liefert der IdP den Mandanten?
- Entscheid nötig: fremder Mandant bei Lese-Endpunkten `200 []` statt `404` (siehe oben), `reject`
  `204` statt `404`. Die P5-Vorgabe „fremde/unbekannte processId → 404" gilt damit nicht mehr.
- `V16`: Backfill der Bestandszeilen mit `default-tenant`; bei einer Umgebung mit abweichendem
  `docextract.tenant.default-id` und laufenden Prozessen müsste das angepasst werden. Die lokale
  Dev-DB hatte die frühere V16 (`audit_entry`) aus dem verworfenen Entwurf; sie wurde manuell
  zurückgenommen (Tabelle, Trigger, Funktion, Flyway-Eintrag).
- Der Primärschlüssel von `extraction_result` und `document_type_candidate` ist nur `process_id`;
  ein Schreibversuch eines anderen Mandanten auf dieselbe ID scheitert mit `DuplicateKeyException`
  (500), überschreibt aber nichts. IDs sind serverseitig erzeugte UUIDs.
- `DocumentBlobPort.describe` filtert nach Mandant; das zusätzliche Benutzer-Prädikat bleibt in
  `PreviewStreamService`.
- Guardrail-Code geändert: `DvelopAuthenticationFilter`, `SessionAuthContextAdapter`,
  `SecurityConfig`, alle Port-Adapter oben — bitte gegenlesen.

---

## 2026-11-10 — Existenzprüfung, Agent-Trennung, Audit, d.velop-Header (Block 3, P5 / Artefakt 4, Rest)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus).

**Auftrag (Prompt-Zusammenfassung):** (1) Existenzprüfung ohne Guard über
`ProcessEventPort.exists(tenantId, processId)`: Services lesen zuerst gefiltert, nur bei leerem
Ergebnis wird `exists` gefragt, sonst `404` (similar-documents, document-type-candidates,
extraction-result, retry, reject, SSE vor dem Stream). (2) Agent-Trennung in `SecurityConfig` über
`docextract.agent.group-id`, Audit-Tabelle `audit_entry` (V17) mit insert-only-Adapter. (3) Echte
d.velop-Mandanten-Header mit `docextract.tenant.app-secret` statt `docextract.tenant.header`.
(4) Primärschlüssel `(tenant_id, process_id)`. (5) `SessionAuthContextAdapter` ohne Authentifizierung
wirft `IllegalStateException`.

**Geänderte Dateien (Auszug):**

- Existenz: `ProcessEventPort.exists`, `PgProcessEventAdapter`, `GetSimilarDocumentsService`,
  `GetDocumentTypeCandidatesService`, `GetExtractionResultService`, `ExtractAttributesService.retry`,
  `RejectSuggestionService`, `ProcessEventsController`; `ProcessNotFoundException` liegt jetzt in
  `process.application`; `RetrievalController`, `ExtractionController`, `ValidationController`
  (OpenAPI: `404` zurück, `doc/api/openapi.yaml` regeneriert)
- Sicherheit: `SecurityConfig` (Agent-Gruppe, Startabbruch bei MCP ohne Gruppe, `none` = keine
  Gruppe), `DvelopAuthenticationFilter` (Einzel-/Mehrmandant, Signaturprüfung vor IdP),
  `TenantSignature`, `RequestTenant(id, baseUri)`, `AgentGroup`, `AuthContext.agent`,
  `SessionAuthContextAdapter`; `DvelopCredential` trägt die Basis-URI (DMS-Adapter und IdP-Aufrufe
  gehen gegen sie)
- Audit: `V17__audit_entry.sql`, `PgAuditAdapter` (ersetzt `NoopAuditAdapter`), `AuditEvent` mit
  Mandant, `AuditChannel`, `ActorKind`, `actor_hash`, `args_hash`; Aufrufe in `AgentGatewayService`,
  `ConfirmAndWriteBackService`, `RejectSuggestionService`, `IngestDocumentService`
- `V16__tenant_on_process_tables.sql` (noch nicht committet, daher erweitert): Primärschlüssel
  `(tenant_id, process_id)` für `extraction_result`, `document_type_candidate`,
  `process_finalization`; `ON CONFLICT` in `PgFinalizationStateAdapter` angepasst
- Tests neu: `TenantSignatureTest` (Testvektor der d.velop-Doku), `TenantResolutionTest`
  (neu geschrieben), `PgAuditAdapterTest` (neu geschrieben), `McpEndpointSecurityTest` (Agent: confirm,
  reject, extraction-result, SSE `403`; Agent/Mensch auf MCP mit Audit-`actor_kind`; Startabbruch;
  `none`); angepasst: `TenantScopedEndpointsTest` (`404` statt leer), `RejectSuggestionServiceTest`,
  `AgentGatewayServiceTest`, `McpToolAdapterTest`, `PgTenantFilterAdapterTest`,
  `PgProcessEventAdapterTest` (`exists`) u. a.
- `doc/ARCHITECTURE.md` § 5.2, § 6.3, § 8.4 (`AUDIT_ENTRY`), § 8.6

**Übernommen (generiert):**

- Basis-URI reist im `DvelopCredential` (kein zusätzlicher Parameter in jedem Port).
- Audit-Fehler lässt die Anfrage scheitern (fail closed); bei `confirm` wird nach dem DMS-Write
  protokolliert (`validation.writeback`).
- Zusätzliche Spalte `token_count` in `audit_entry` (für LLM-Ereignisse); `tenant_id`, `actor_kind`,
  `actor_hash` sind für Systemereignisse (LLM-Prompt-Hash, TTL-Bereinigung) `NULL`.
- Einzelmandant: Basis-URI weiter über `AppConfigPort` (jstore zuerst, sonst
  `docextract.dvelop.system-base-uri`). Dev-Profil setzt `docextract.agent.group-id: none`.
- Live-Smoke (`dev`, MCP an, Secret gesetzt): V16/V17 migriert, Start ohne Fehler; Request ohne
  Signatur `403`, mit Testvektor-Signatur weiter zum IdP (`401`, IdP nicht erreichbar).
- `mvn -o clean verify` grün (190 Tests, ArchUnit grün).

**Zu prüfen durch Mensch:**

- `process_step` wird nie bereinigt (unbegrenztes Wachstum) und ist damit länger aufbewahrt als die
  Ergebnistabellen; eine Aufbewahrungsregel fehlt.
- `BLOB_STORED` ist vor dem `202` nicht garantiert committet (asynchrone Kette); `DMS_UPLOADED` ist es.
  Ein sofortiges `GET` direkt nach dem `202` findet den Prozess über `DMS_UPLOADED`.
- `actor_hash` ist ungesalzen (SHA-256 über Mandant und User-ID), bei bekannten User-IDs rückrechenbar.
- `ValueListLookupTool` und `ExtractAttributesService.audit` schreiben `SYSTEM`-Ereignisse ohne
  Mandant; ob der Mandant dort ergänzt werden soll, ist offen.
- Header werden nur bei gesetztem App-Secret ausgewertet; ohne Secret im Cloud-Betrieb liefe alles
  unter dem Default-Mandanten.
- Lokale Dev-DB: die frühere V16 wurde manuell zurückgenommen (Spalten, Flyway-Eintrag) und neu
  migriert. Der Stash `P5-attempt…` besteht weiter.
- Guardrail-Code geändert: `SecurityConfig`, `DvelopAuthenticationFilter`, `TenantSignature`,
  `SessionAuthContextAdapter`, `PgAuditAdapter` — bitte gegenlesen.

---

## 2026-11-10 (2) — P5-Abschluss Teil 2: Retention, Basis-URI, Audit-Schlüssel, Confirm-Reihenfolge (Block 3, P5 / Artefakt 4)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus).

**Auftrag (Prompt-Zusammenfassung):** Die offenen Punkte des Eintrags oben schliessen, je Punkt mit
Test, V16/V17 (noch nicht committet) in place: (1) `process_step`-Retention als `TtlPurger` mit
Startprüfung, (2) Test „`exists` ist nach dem `202` wahr“, (3) im Mehrmandantenbetrieb kein Fallback
auf `AppConfigPort`, (4) `provider`/`model_id` in `audit_entry`, (5) LLM-Ereignisse mit Mandant,
(6) `actor_hash` als HMAC mit eigenem Schlüssel, (7) Confirm-Reihenfolge mit
`validation.writeback-started`, (8) ARCHITECTURE an den Code angleichen.

**Geänderte Dateien:**

- Retention: `ProcessStepRetentionService` (validation, `TtlPurger`; Startprüfung im Konstruktor),
  `ProcessEventPort.deleteStepsOfIdleProcesses`, `PgProcessEventAdapter`, `application.yml`
  (`docextract.process.step-retention: PT48H`). Tests: `ProcessStepRetentionServiceTest`,
  `PgProcessEventAdapterTest` (Löschen, Ausnahme, Prozess mit jüngstem Schritt bleibt)
- Existenz nach `202`: `IngestDocumentServiceTest.process_exists_for_its_tenant_as_soon_as_ingest_returns`
  (Preview-Future bleibt offen, `DMS_UPLOADED` genügt)
- Basis-URI: `DvelopCredential` verweigert fehlende/leere Basis-URI, `DvelopAuthenticationFilter` lehnt
  einen leeren signierten `x-dv-baseuri` mit `403` ab. Tests in `TenantResolutionTest` (kein Zugriff
  auf `AppConfigPort` im Mehrmandantenbetrieb)
- Audit: `V17__audit_entry.sql` (`provider`, `model_id`), `AuditEvent` (`llm(...)`, `ttlCleanup()`,
  kein öffentlicher 3-Argument-Konstruktor mehr), `PgAuditAdapter`, `ExtractAttributesService` und
  `ValueListLookupTool` (Mandant aus der Pipeline), `TtlCleanupService`. Tests:
  `ExtractAttributesServiceTest` (neu, auch `retry` → `404`), `ValueListLookupToolTest`,
  `PgAuditAdapterTest`
- Actor-Hash: `shared.hash.Hmac`, `AuthContext.actorHash` (Feld statt Methode),
  `SessionAuthContextAdapter(agentGroupId, actorKey, appSecret)`, `application.yml`
  (`docextract.audit.actor-key`). Tests: `SessionAuthContextAdapterTest` (neu),
  `McpEndpointSecurityTest`, `AgentGatewayServiceTest`
- Confirm: `ConfirmAndWriteBackService` (`validation.writeback-started` vor Marker und DMS-Write;
  nach dem Write erst Saga-Zustand, dann `validation.writeback`, Fehler dort nur `ERROR`-Log).
  Tests: `ConfirmAndWriteBackServiceTest` (Reihenfolge, fail closed vor dem Write, Audit-Fehler nach
  dem Write → Zustand gespeichert, Antwort ok, kein zweiter Write)
- `doc/ARCHITECTURE.md` § 2, § 3, § 3.1, § 5.2, § 5.4, § 5.5, § 6.1, § 6.3, § 8.2, § 8.3, § 8.4,
  § 8.6, § 10.3

**Übernommen (generiert):**

- Die Startprüfung der Retention steht im Konstruktor des Purgers (liest die TTL-Properties mit den
  gleichen Defaults wie die Adapter) statt in einer eigenen Config-Klasse. Geprüft werden Blob-,
  Retrieval-Result-, Pending-Embedding-, Extraction-Result-TTL und `promotion-retry-limit`; die
  in-memory Staging-TTL (`PT30M`) ist nicht dabei.
- Der Purger löscht einen Prozess erst, wenn sein _neuester_ Schritt älter als die Retention ist (kein
  halber SSE-Verlauf). Er überspringt Prozesse in `FINALIZED_INDEX_PENDING` nach `process_id`.
- Der Actor-Schlüssel ist ein Klartext-String (kein Base64). Ohne App-Secret und ohne Schlüssel gilt
  ein fester Dev-Schlüssel mit `WARN` im Log; mit Secret und ohne Schlüssel, oder mit Schlüssel
  gleich Secret, bricht der Start ab.
- `AuthContext` behält Konstruktoren ohne `actorHash` (nur für Tests); der Hash ist dann `null`.
- Zusätzlich zur Vorgabe: auch ein Fehler beim Audit `validation.writeback-rejected` wird nur
  geloggt, damit die Marker-Bereinigung (`discardWriteStarted`, jetzt vor dem Audit) und die
  ursprüngliche Fehlerantwort erhalten bleiben.
- Tool-Aufrufe (`extraction.tool.lookup_value_list`) tragen Mandant, aber weder Anbieter noch Modell.
- Hinweis zum Build: ein inkrementelles `mvn test-compile` erkannte geänderte Signaturen in Tests
  nicht; erst `mvn clean test-compile` zeigte die Fehler.

**Zu prüfen durch Mensch:**

- Die Retention-Abfrage gruppiert `process_step` nach `process_id` ohne Index auf `created_at`; bei
  sehr vielen Zeilen messen.
- Der Purger skippt nach `process_id` allein (nicht nach Mandant und Prozess); Prozess-IDs sind
  UUIDs, die Wahrscheinlichkeit einer Kollision ist vernachlässigbar.
- Eine Rotation von `docextract.audit.actor-key` macht alte und neue `actor_hash` nicht mehr
  vergleichbar.
- Dev-Profil und Smoke-Boot laufen weiter mit dem Dev-Schlüssel (`WARN`); für die Cloud
  `DOCEXTRACT_AUDIT_ACTOR_KEY` setzen.
- Guardrail-Code geändert: `ProcessStepRetentionService`, `ConfirmAndWriteBackService`,
  `SessionAuthContextAdapter`, `DvelopCredential`, `DvelopAuthenticationFilter` — bitte gegenlesen.

---

## 2026-11-10 (3) — P5-Abschluss Teil 3: Index, Actor-Key-Pflicht, Doku-Angleichung (Block 3, P5 / Artefakt 4)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus).

**Auftrag (Prompt-Zusammenfassung):** (1) V16 (noch nicht committet, in place): Index auf
`process_step (process_id, created_at)`. (2) `docextract.audit.actor-key`: Dev-Schlüssel nur im Profil
`dev`, sonst Pflicht (Startabbruch), Mindestlänge 32, je Fall ein Test. (3) ARCHITECTURE.md angleichen
(§ 3.1, § 5.4/§ 5.5, ADR-002, ADR-015, § 6.2, § 11, § 8.6). (4) CONTEXT.md: „grün" nur nach
`mvn -o clean verify`.

**Geänderte Dateien:**

- `V16__tenant_on_process_tables.sql`: `idx_process_step_process_created`. Test:
  `PgProcessEventAdapterTest.retention_query_is_backed_by_an_index_on_process_id_and_created_at`
- `SessionAuthContextAdapter` (neuer Parameter `Environment`; Dev-Schlüssel nur bei Profil `dev`,
  sonst Startabbruch; Mindestlänge 32 in jedem Profil; mit App-Secret auch im Profil `dev` Pflicht;
  Schlüssel ≠ Secret), `application.yml`. Tests: `SessionAuthContextAdapterTest` (neu geschrieben, je
  Fall), `McpEndpointSecurityTest` (echter Start: ohne Schlüssel Abbruch, zu kurzer Schlüssel Abbruch,
  Profil `dev` startet), `TenantResolutionTest`
- `doc/ARCHITECTURE.md`: § 3.1 (Zeile IdP korrigiert, Agent-Zeile: Agent-Gruppe, nur MCP, nur lesend),
  § 5.4/§ 5.5 („Mandantenfilter + `exists`" statt „Ownership"), § 6.2 (Diagramm mit
  `writeback-started` vor dem DMS-Write), § 6.3, § 8.6 (Actor-Key-Regeln, Purger-Skip über
  `process_id`, Index-Punkt aus „Offen" entfernt), ADR-002 und ADR-015 (Schritt 0: signierter Header
  bzw. Einzelmandant-Default; Audit-Schritte), § 11 (`process_step` in der Cleanup-Liste)
- `doc/CONTEXT.md` § 8: Regel zur Meldung „grün"

**Übernommen (generiert):**

- Dev-Schlüssel jetzt `docextract-dev-actor-key-not-for-production` (44 Zeichen, damit er die
  Mindestlänge erfüllt); `WARN` im Log bleibt.
- Die Mindestlänge gilt auch im Profil `dev`, nur ein fehlender Schlüssel fällt dort zurück.
- Die Dev-DB musste für V16 und V17 zurückgenommen werden (Spalten, Primärschlüssel, `audit_entry`,
  Flyway-Einträge 16 und 17), da Flyway sonst die V16-Prüfsumme bemängelt.
- Beim Durchsehen von § 3.1 fiel auf, dass meine frühere Änderung die IdP-Zeile um eine Spalte
  verkürzt hatte; korrigiert.

**Zu prüfen durch Mensch:**

- Wer das Profil `dev` in einer Umgebung mit echten Daten aktiviert, bekommt den bekannten Dev-Schlüssel
  (nur `WARN`); das Profil ist als lokale Entwicklung gedacht.
- Bestehende Deployments ohne `DOCEXTRACT_AUDIT_ACTOR_KEY` starten nach diesem Stand nicht mehr.
- Bei `docker-compose.yml`/README wurde der neue Pflichtparameter nicht ergänzt.
- Guardrail-Code geändert: `SessionAuthContextAdapter` — bitte gegenlesen.

---

## 2026-11-10 (4) — FR-3/FR-4 Umsetzungsstand geprüft, Pre-Filter und Retry ergänzt (Block 3, 3.6)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus).

**Auftrag (Prompt-Zusammenfassung):** Umsetzungsstand von FR-3/FR-4 hinter `EmbeddingPort`/`LlmPort`
prüfen und als Tabelle festhalten; nur eindeutig in SPEC/ARCHITECTURE beschriebene Lücken minimal
schliessen; Tests (LLM gemockt) für ungültiges-JSON-Retry, Wert-ausserhalb-Werteliste, Cross-Tenant;
Contract-Test-Suite für `LlmPort` anlegen, falls fehlend; PROJEKTPLAN.md 3.6 mit den Befunden
nachführen.

**Befund (1): Umsetzungsstand**

| Baustein | Status | Befund |
| --- | --- | --- |
| `LlmPort` + OpenAI-kompatibler Adapter | ✅ vorhanden | `LlmPort`, `OpenAiCompatibleLlmAdapter`; ein generischer Adapter für lokal/extern (ADR-011), kein zweiter konkreter Adaptertyp (NfA-8 "≥ 2 Adapter" bleibt über Konfiguration, nicht über Code, erfüllt) |
| `EmbeddingPort` + Adapter | ✅ vorhanden | `EmbeddingPort`, `OpenAiCompatibleEmbeddingAdapter` |
| Serverseitige JSON-Schema-Validierung mit Retry gemäss `max-retries` | ⚠️ war teilweise → jetzt ✅ | Jackson-Deserialisierung validierte bereits strukturell, aber ohne Retry: ein ungültiges LLM-JSON führte sofort zum Abbruch. Ergänzt: `ExtractAttributesService#generateValidated` ruft das LLM bei einer ungültigen Antwort bis zu `docextract.adapters.llm.max-retries` (neu, Default 1) weitere Male auf, auditiert jeden Versuch, und endet danach kontrolliert in `LlmException` → `ExtractionException` → `FAILED` (ARCHITECTURE § 6.5) |
| „unbekannt"-Pfad | ✅ vorhanden | `ExtractAttributesService#resolveOne`: ein `hasValueList`-Vorschlag ohne Treffer wird mit `value=null, confidence=0.0` abgelehnt statt übernommen (E-5), bereits mit Test belegt |
| Audit mit Modell-ID/Token | ✅ vorhanden | `ExtractAttributesService#audit`, `StagePendingEmbeddingsService#audit`: SHA-256 des Prompts/Texts, Tokenzahl, Anbieter, Modell-ID, nie der Rohtext (C-4) |
| Pre-Filter-Query (`tenant_id`, `APPROVED`, `embedding_model`, `extraction_source`) | ⚠️ war teilweise → jetzt ✅ | `PgVectorSearchAdapter`/`VectorSearchPort` filterten nur `tenant_id` + `status='APPROVED'`; `embedding_model`/`extraction_source` fehlten als SQL-Prädikat (nur implizit über `ReindexService`, das einen abgelösten `embedding_model` auf `STALE` setzt). Ergänzt: beide Spalten sind jetzt Pflichtparameter von `VectorSearchPort#search` und Teil der `WHERE`-Klausel; `FindSimilarService` übergibt die konfigurierten Werte (`docextract.retrieval.embedding-model`, `docextract.adapters.text-extraction`) |

**Beobachtung (nicht behoben, ausserhalb des Auftrags):** Die DB-`CHECK`-Constraint `chk_model` auf
`embedding.embedding_model` (`V7__embedding_source_model.sql`) erlaubt aktuell nur den einen Wert
`qwen3-embedding:0.6b` und widerspricht damit dem in `ReindexService`/ADR-012 beschriebenen
Mehr-Modell-Re-Indexierungsszenario (ein zweites Modell könnte gar nicht erst eingefügt werden). War
nicht Teil des Auftrags; siehe „Zu prüfen durch Mensch".

**Umgesetzt (2):**

- `ExtractAttributesService`: neuer Konstruktorparameter `maxRetries`
  (`@Value("${docextract.adapters.llm.max-retries:1}")`), neue private Methode `generateValidated`
  (generate → Jackson-Parse → bei Fehler bis zu `maxRetries` weitere Versuche, jeder auditiert; danach
  `LlmException`), beide LLM-Aufrufstellen (`classifyDocumentType`, `suggestAttributes`) darauf
  umgestellt. `application.yml`: `docextract.adapters.llm.max-retries: 1` dokumentiert.
- `VectorSearchPort#search` um `embeddingModel`/`extractionSource` erweitert; `PgVectorSearchAdapter`
  filtert beide in der `WHERE`-Klausel; `FindSimilarService` liest beide aus der bestehenden
  Konfiguration und reicht sie durch.

**Tests (3):**

- `ExtractAttributesServiceTest.invalid_llm_json_is_retried_then_ends_in_a_controlled_failure`:
  durchgehend ungültiges JSON → genau 2 LLM-Aufrufe (1 + `max-retries`=1), 2 Audit-Einträge,
  `ExtractionException` mit `LlmException`-Ursache, `FAILED`-Event, kein `ExtractionResult` gespeichert.
- `ExtractAttributesServiceTest.llm_suggested_value_outside_the_known_value_list_is_rejected_as_unknown`:
  LLM schlägt einen Wert vor, der nicht in der bekannten Werteliste steht → `ExtractedAttribute` mit
  `value=null, confidence=0.0` (bereits vorhandenes Verhalten, jetzt direkt mit Test belegt).
- `PgVectorSearchAdapterTest.finds_only_approved_embeddings_for_the_caller_s_tenant` (bereits
  vorhanden, nur Signatur angepasst) und neu
  `excludes_approved_embeddings_of_a_different_extraction_source_or_embedding_model`: Cross-Tenant
  bzw. abweichender `extraction_source`/`embedding_model` → 0 Treffer, mit echtem Postgres/pgvector
  (Testcontainers).
- `IngestToSuggestionsWithMockLlmTest` (neu): `StagePendingEmbeddingsService` →
  `FindSimilarService` → `ExtractAttributesService` end-to-end mit handgeschriebenem Mock-`LlmPort`
  und Mock-`EmbeddingPort` (kein Netzwerk/Modell nötig) — ein Dokument erreicht über die echten
  Produktions-Services einen Attributvorschlag. Deckt die Akzeptanz sinngemäss ab; siehe „Zu prüfen
  durch Mensch" zum Umfang.

**Contract-Test-Suite LlmPort (4):**

- Fehlte vollständig (nur `OpenApiContractTest` existierte projektweit). Neu angelegt:
  `LlmPortContractTest` (abstrakt, in `extraction.port`): prüft, dass `generate(...)` parsebares JSON
  mit Modell-Identität und Tokenzahl liefert und `capabilities()` nicht `null` ist.
  `OpenAiCompatibleLlmAdapterContractTest` lässt den vorhandenen Adapter diese Suite erfüllen, gegen
  einen In-Prozess-`HttpServer`-Stub (JDK, keine neue Abhängigkeit), der das
  OpenAI-kompatible Chat-Completions-Protokoll nachbildet — kein echtes Modell/Netzwerk nötig.

**Geänderte Dateien:**

- `ExtractAttributesService.java`, `VectorSearchPort.java`, `PgVectorSearchAdapter.java`,
  `FindSimilarService.java`, `application.yml`
- Tests: `ExtractAttributesServiceTest.java`, `FindSimilarServiceTest.java`,
  `PgVectorSearchAdapterTest.java`, neu `LlmPortContractTest.java`,
  `OpenAiCompatibleLlmAdapterContractTest.java`, `IngestToSuggestionsWithMockLlmTest.java`
- `doc/PROJEKTPLAN.md` § 5 (3.6-Status)

**Build:** `mvn -o clean verify` grün (233 Tests, inkl. ArchUnit, `OpenApiContractTest`,
Testcontainers-Postgres-Tests).

**Zu prüfen durch Mensch:**

- OFFEN: Default von `docextract.adapters.llm.max-retries` (hier `1`, d. h. 2 Versuche gesamt) ist
  eine Implementierungsannahme, kein SPEC-Wert — bitte gegen das reale Fehlerbudget (NfA-3,
  Fehlerinjektions-Testsuite) prüfen.
- OFFEN: Die `chk_model`-Constraint (siehe Beobachtung oben) verhindert aktuell technisch jeden
  zweiten `embedding_model`-Wert in der DB und damit das in ADR-012/`ReindexService` beschriebene
  Re-Indexierungsszenario; nicht angefasst, da ausserhalb des engen Auftrags (Pre-Filter-Query).
- OFFEN: Das Akzeptanzkriterium „Ein Dokument durchläuft im Profil `ci` (Mock-LLM) Upload →
  Vorschläge" ist mit `IngestToSuggestionsWithMockLlmTest` auf Service-Ebene belegt (reale
  `StagePendingEmbeddingsService`/`FindSimilarService`/`ExtractAttributesService` mit
  Mock-`LlmPort`/`EmbeddingPort`), **nicht** als HTTP-Ende-zu-Ende-Test über den REST-Ingest-Endpunkt
  unter dem Spring-Profil `ci`. Im Repo existiert aktuell kein einziger `@SpringBootTest`; ein solcher
  Harness (plus Mock-DMS, Mock-LLM/-Embedding als echte `@Profile("ci")`-Beans) wäre ein
  eigenständiges, grösseres Vorhaben und keine „minimale Ergänzung" — bitte entscheiden, ob das
  nachgezogen werden soll.
- NfA-8 bleibt mit nur einem konkreten `LlmPort`-Adaptertyp (OpenAI-kompatibel, lokal/extern nur über
  Konfiguration unterschieden) formal "1 Adaptertyp, 2 Konfigurationen" statt zweier unabhängiger
  Adapterimplementierungen; wie in ARCHITECTURE § 9 (ADR-011) bereits so entschieden, hier nur erneut
  festgehalten.

---

## 2026-11-10 (5) — V18, Full-Stack-Test unter Profil `ci`, parametrisierte LlmPort-Suite (Block 3, 3.6)

**Tool/Modell:** GitHub Copilot Chat (Agent-Modus).

**Auftrag (Prompt-Zusammenfassung):** (1) Migration V18: `chk_model` entfernen, Vektordimension und
Status-Constraint prüfen und nur melden, Test mit zwei `embedding_model`-Werten. (2) `@SpringBootTest`
(Profil `ci`, Testcontainers-Postgres, DMS/IdP-Mock, In-Prozess-Stub für LLM/Embedding):
Upload → 202 → `extraction-result`. (3) `IngestToSuggestionsWithMockLlmTest` umbenennen oder entfernen.
(4) `LlmPortContractTest` mit zwei Konfigurationen. (5) Doku nachführen.

**Korrektur am Vorgehen (Veto):** Ich hatte zunächst die Test-Dependency `org.wiremock:wiremock` in die
`pom.xml` aufgenommen. Das Repo hat bereits WireMock-Images (`docker/dvelop-mock`, `docker/llm-mock`);
die Dependency wurde wieder entfernt. Stattdessen baut der Test das vorhandene `docker/dvelop-mock` per
Testcontainers (`ImageFromDockerfile`, schon vorhandene Testcontainers-Core-Klasse). **Es kamen keine
neuen Dependencies hinzu** (Awaitility und `junit-jupiter-params` sind transitiv über
`spring-boot-starter-test` da).

**Befund Schema (1):**

| Frage | Befund |
| --- | --- |
| Vektorspalte fest dimensioniert? | **Ja**: `embedding vector(1024)` (V3). Ein Modell mit anderer Dimension lässt sich nicht einfügen, auch ohne `chk_model` nicht. |
| Status-Constraint, der `STALE` verhindert? | **Nein**: `status` ist `VARCHAR(16)` ohne `CHECK`; `STALE` (`ReindexService`) war schon vorher zulässig. |

- OFFEN: Multi-Modell-Re-Indexierung (ADR-012) bleibt für andere Dimensionen blockiert; nötig wäre eine
  eigene Migration (Dimension je Modell, z. B. eigene Spalte/Tabelle). Nicht geändert, nur gemeldet.
- OFFEN: Der Index `idx_embedding_approved_tenant` und der HNSW/IVFFlat-Index (falls später) gelten für
  genau eine Dimension; bei Modellwechsel mitprüfen.

**Umgesetzt:**

- `V18__embedding_drop_chk_model.sql` (V7 unverändert). Test
  `PgVectorSearchAdapterTest.pre_filter_separates_two_distinct_embedding_model_values_in_the_same_corpus`
  (Testcontainers): zwei `embedding_model`-Werte einfügbar, Pre-Filter trennt sie; der ältere Test
  wurde auf echte zwei Modelle umgestellt.
- `IngestToExtractionResultHttpTest` (neu, `@SpringBootTest`, Profil `ci`, Zufallsport): echte
  Anwendung gegen Testcontainers-pgvector, das Repo-Image `docker/dvelop-mock` (IdP, objdef,
  Chunk-Upload, Werteliste) und `OpenAiCompatibleStub`. `POST /api/v1/ingest/documents` → `202` +
  `processId` → Polling auf `GET /processes/{id}/extraction-result` → Dokumenttyp `DREC` und Attribut
  `11`. Zusätzlich belegt: 1 Embedding-Aufruf, 2 Chat-Aufrufe, 1 `PENDING`-Vektor im Mandanten
  `default-tenant`, 2 Audit-Einträge mit Modell-ID, Tokenzahl und 64-stelligem Hash (kein Klartext).
  Keine Mock-Beans, kein Test-Code im Produktivcode.
- `OpenAiCompatibleStub` (Test-Sources, `shared.testsupport`): aus dem Contract-Test herausgelöst und
  für beide Tests wiederverwendet (JDK-`HttpServer`, antwortet auf `/chat/completions` und
  `/embeddings`, Chat-Antwort aus dem Request abgeleitet).
- `IngestToSuggestionsWithMockLlmTest` **entfernt**: der Startpunkt war nicht der Upload, und der neue
  Test deckt Upload → Vorschläge vollständig ab (mit echten Adaptern statt Hand-Fakes). Verweis in
  `docker/llm-mock/README.md` angepasst. (Frühere Tagebuch-Einträge nennen ihn historisch weiter.)
- `LlmPortContractTest` jetzt parametrisiert (`@ParameterizedTest`, `configurations()`); ausgeführt mit
  „lokal (ohne api-key)" und „extern (api-key + api-version)" (4 Läufe).

**Geänderte Dateien:** `V18__embedding_drop_chk_model.sql`, `PgVectorSearchAdapterTest.java`,
`LlmPortContractTest.java`, `OpenAiCompatibleLlmAdapterContractTest.java`, neu
`OpenAiCompatibleStub.java`, `IngestToExtractionResultHttpTest.java`, gelöscht
`IngestToSuggestionsWithMockLlmTest.java`, `docker/llm-mock/README.md`, `doc/PROJEKTPLAN.md`.

**Build:** `mvn -o clean verify` grün (236 Tests).

**Zu prüfen durch Mensch:**

- OFFEN: Dimensions-Frage siehe oben (Multi-Modell mit anderer Dimension).
- OFFEN: Der Full-Stack-Test braucht Docker (pgvector-Image, Bau von `docker/dvelop-mock` inkl. Pull des
  WireMock-Basis-Images). Das passt zu den bestehenden Testcontainers-Tests, verlängert aber den Build
  um etwa 10 s und braucht in der CI Zugriff auf das Basis-Image.
- OFFEN: Der Stub antwortet auf Embedding immer mit demselben Vektor; Retrieval-Güte (NfA-6) und
  Treffer aus dem `APPROVED`-Korpus werden damit nicht geprüft (nur der Pfad ohne ähnliche Dokumente).
- Der Test nutzt `docextract.dvelop.system-base-uri` mit absichtlich 404 liefernder jstore-URI statt
  des echten jstore-Mocks (dessen `systembaseuri` zeigt auf `localhost:6380`).
