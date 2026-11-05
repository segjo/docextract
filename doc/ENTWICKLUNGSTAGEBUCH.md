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
