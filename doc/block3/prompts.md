# Block 3 – Prompts für den Coding Agent (DocExtract)

Reihenfolge einhalten: Jeder Prompt baut auf dem vorherigen auf. Pro Prompt eine eigene Session und einen eigenen Commit bzw. PR.
Den **gemeinsamen Rahmen** jedem Prompt voranstellen.

---

## Gemeinsamer Rahmen (jedem Prompt voranstellen)

```text
Projekt: DocExtract (Java 21 / Spring Boot 4, Angular, PostgreSQL + pgvector, Spring AI).
Massgebliche Dokumente: doc/SPEC.md, doc/ARCHITECTURE.md, doc/PROJEKTPLAN.md, doc/block3/tasks.md,
doc/ENTWICKLUNGSTAGEBUCH.md. Lies sie vor jeder Änderung.

Regeln:
- Keine neuen Anforderungen erfinden. Was SPEC/ARCHITECTURE nicht festlegt und du entscheiden
  müsstest: als "OFFEN: <Frage>" markieren, nicht raten.
- Hexagonal: keine Produkt-/Framework-Typen in domain/application/port (ArchUnit muss grün bleiben).
- Guardrails sind unverhandelbar: C-2 (kein Write ohne menschliche Freigabe), C-3 (Least Privilege),
  C-4 (Audit ohne Roh-PII), NfA-4 (tenant_id als Pre-Filter).
- Bestehende Tests dürfen nicht rot werden. Build: Backend `mvn verify`, Frontend Tests wie im Repo üblich.
- Doku: Deutsch, Markdown, knapp. Bestehende Abschnittsnummern in ARCHITECTURE.md nicht verschieben,
  nur ergänzen.
- Am Ende: Eintrag in doc/ENTWICKLUNGSTAGEBUCH.md (Datum, Prompt-Kurzfassung, geänderte Dateien,
  was du generiert hast) und eine Liste "Zu prüfen durch Mensch" mit allen OFFEN-Punkten.
```

---

## P0 – Doku-Konsistenz bereinigen (Vorarbeit)

```text
Aufgabe: Widersprüche in doc/ARCHITECTURE.md beheben, ohne Inhalte fachlich zu ändern.

1. § 5.4 (Fachmodule) an § 5.2/§ 5.5 angleichen: statt "VectorStorePort" und "DmsReadPort"
   die schmalen Ports verwenden (VectorSearchPort, PendingEmbeddingPort, CorpusPromotionPort,
   PendingEmbeddingDeletePort, DmsObjectDefinitionPort, DmsObjectMetadataPort).
2. Zuordnung von ValueListPort, DmsObjectDefinitionPort, DmsObjectMetadataPort klären: Prüfe im
   Code (Package ch.adeon.apps.docextract), in welchem Modul sie liegen, und passe § 5.4 UND § 5.5
   an den Code an. Wenn Code und Doku auseinanderlaufen: Doku folgt dem Code, Abweichung im
   Tagebuch notieren.
3. Prüfe, ob alle in § 5.2 genannten Ports im Code existieren. Fehlende: als Tabelle
   "Port | in Doku | im Code" im Tagebuch auflisten, NICHT implementieren.

Akzeptanz: Kein Port-Name kommt in ARCHITECTURE.md vor, der nicht in § 5.2 oder im Code existiert.
```

---

## P1 – TRANSFER-Entscheid als ADR-015 (Artefakt 0 + Grundlage für 5)

```text
Aufgabe: Neues ADR-015 in doc/ARCHITECTURE.md § 9 für die Naht validation → retrieval
(CorpusPromotionPort) bzw. validation → DMS (DmsWritePort), d. h. die Freigabe-Saga aus § 6.2.

Format (genau diese Überschriften):
- Status, Kontext
- Entscheidung: wie die zwei Module reden (synchron, in-process, über Port)
- Preis: was die Entscheidung kostet (z. B. Zwischenzustand FINALIZED_INDEX_PENDING,
  Kopplung an gemeinsame Transaktion/DB, Retry-Logik)
- Störfall: konkretes Szenario "DMS-Finalisierung ok, Promotion schlägt fehl" + Instanzausfall
  dazwischen; erwartetes Systemverhalten
- Prüfung: welche Tests das Verhalten belegen (Testklassen-Namen)
- Alternativen: Choreografie (Event, z. B. via PROCESS_STEP/NOTIFY) vs. Orchestrierung
  (ConfirmAndWriteBackService); je 2–3 Sätze, warum verworfen/gewählt
- Konsequenz

Danach im Code:
1. Prüfe, ob ConfirmAndWriteBackService die Reihenfolge DMS-Write → Promotion einhält und bei
   Promotion-Fehler FINALIZED_INDEX_PENDING setzt. Wenn nicht vorhanden: implementieren.
2. Tests schreiben/ergänzen (Unit mit gemockten Ports):
   a) Promotion-Fehler → Status FINALIZED_INDEX_PENDING, kein zweiter DMS-Write.
   b) Retry im Zustand FINALIZED_INDEX_PENDING → nur Promotion, DMS-Write wird NICHT erneut aufgerufen.
   c) TTL-Cleanup überspringt FINALIZED_INDEX_PENDING.
   d) PENDING-Embedding erscheint nie im Retrieval (falls noch nicht vorhanden).
3. Testklassen-Namen im ADR unter "Prüfung" eintragen.

Akzeptanz: ADR-015 vollständig, Tests a–d grün, ArchUnit grün.
```

---

## P2 – Fachliche Zerlegung (Artefakt 1)

```text
Aufgabe: Neuer Abschnitt "§ 5.6 Modulschnitt und Verträge" in doc/ARCHITECTURE.md.

1. Mermaid-Diagramm (flowchart) der Fachmodule ingest, content, retrieval, extraction, validation,
   agentgateway, process, audit, security mit den Port-Abhängigkeiten zwischen ihnen
   (Kantenbeschriftung = Port-/Service-Name). Grundlage ist der tatsächliche Code, nicht nur die Doku.
2. Für mindestens zwei Nähte (Pflicht: validation→retrieval, extraction→retrieval) je 2–3 Sätze:
   Änderungsgrund, Kopplung, Resilienz.
3. Ein Satz mit Verweis auf ADR-001: verteilt wird nicht, weil keine Qualitätsanforderung es verlangt.
4. Eine ArchUnit-Regel prüfen/ergänzen, die Zugriffe zwischen Modulen nur über application/port
   erlaubt (kein Zugriff auf domain-Interna oder adapter eines anderen Moduls). Bestehende Verletzungen
   auflisten, nicht stillschweigend unterdrücken.

Akzeptanz: Diagramm stimmt mit Code überein; ArchUnit-Regel vorhanden und grün (oder Verletzungen
dokumentiert als OFFEN).
```

---

## P3 – OpenAPI mit Fehlerfällen (Artefakt 2)

```text
Aufgabe: Maschinenlesbaren API-Vertrag erstellen.

1. Prüfe, ob springdoc-openapi eingebunden ist. Ziel: versionierte Datei doc/api/openapi.yaml
   (OpenAPI 3.1). Falls springdoc vorhanden: generieren und als Datei committen; zusätzlich einen
   Test, der die generierte Spec gegen die committete Datei vergleicht (Drift-Check).
   Falls nicht vorhanden: Datei manuell aus den Controllern ableiten, KEINE neue Dependency ohne
   Rückfrage (als OFFEN markieren).
2. Alle öffentlich sichtbaren HTTP-Endpunkte (Upload, Preview inkl. Range, ImportInfo, SSE
   /processes/{id}/events, confirm, reject, ggf. weitere aus dem Code).
3. Fehlerfälle je Endpunkt als application/problem+json (RFC 9457) mit gemeinsamem Schema
   "Problem" in components. Mindestens: 400, 401, 403, 404, 409, 413, 422, 503/504 wo zutreffend.
   Nur Codes dokumentieren, die der Code tatsächlich liefert oder nach P7 liefern wird.
4. Zentraler Endpunkt POST /documents/{id}/confirm vollständig, per x-Extensions + description:
   - x-idempotency: Regel ausformulieren (Wiederholung bei bereits finalisiertem bzw.
     FINALIZED_INDEX_PENDING-Prozess → kein zweiter DMS-Write; Antwortcode festlegen, z. B. 200 mit
     gleichem Ergebnis oder 409 – im Code prüfen, was gilt; sonst OFFEN).
   - x-data-classification: Request enthält Attributwerte = potenziell Personendaten; Logging/Audit
     nur ohne Roh-PII (C-4).
   - x-slo: Platzhalter "OFFEN: Zielwert p95 und Verfügbarkeit durch Mensch festlegen" – KEINEN Wert
     erfinden.
5. Idempotenz-Test für confirm (Integration oder WebMvc): zweimaliger Aufruf → DmsWritePort genau 1×.
6. In ARCHITECTURE.md § 8.1 Verweis auf doc/api/openapi.yaml ergänzen.

Akzeptanz: openapi.yaml validiert (z. B. mit vorhandenem Linter/Parser-Test), Idempotenz-Test grün.
```

---

## P4 – MCP-Tool (Artefakt 3)

```text
Aufgabe: Modul agentgateway (adapter.in.mcp) mit mindestens einem lauffähigen MCP-Tool.

Vorgaben:
- Spring AI MCP Server innerhalb des Adapters (ADR-009); keine Spring-AI-Typen ausserhalb adapter.
- Tools rufen DIESELBEN Application-Services wie REST (FindSimilar, ExtractAttributes bzw. Abfrage
  eines vorhandenen Prozess-Ergebnisses). Keine Logik im Tool duplizieren.
- Nur lesende Tools. Vorschlag:
  1. get_process_suggestions(processId) → Vorschläge inkl. Konfidenz, Modell-ID, lokal/extern
  2. find_similar_documents(processId) → Top-5 (document_id, repository_id, Score), nur APPROVED
- KEIN Tool für confirm/DMS-Write (C-2, ADR-003). Im Tool-Description darauf hinweisen, aber die
  Durchsetzung liegt im Service, nicht in der Beschreibung.
- Eingabeschema je Tool (JSON Schema), strikte Validierung (UUID-Format etc.).
- Identität/Mandant des Agenten: Prüfe, ob es eine bestehende Lösung gibt. Wenn nicht: Agent erhält
  einen konfigurierten, mandantengebundenen Service-Token (Env-Variable), aus dem AuthContextPort
  die tenant_id liefert. Diese Annahme als OFFEN markieren und im Tagebuch begründen.
- Transport: wie von Spring AI MCP Server unterstützt (SSE/Streamable HTTP); per Property
  docextract.adapters.mcp.enabled abschaltbar (default false).

Tests:
- Tool-Aufruf mit processId eines fremden Mandanten → Fehler, keine Daten (NfA-4).
- Ungültige Eingabe → Validierungsfehler.
- Jeder Tool-Call erzeugt einen Audit-Eintrag (siehe P5).
- ArchUnit grün.

Doku: Kurzbeschreibung in ARCHITECTURE.md § 6.3 (Toolnamen, Ein-/Ausgabe, Transport, AuthN),
README-Abschnitt "MCP-Tool lokal testen" mit einem Beispielaufruf.
```

---

## P5 – Sicherheitskonzept Agent und Werkzeug (Artefakt 4)

```text
Aufgabe: Abschnitt "§ 8.6 Sicherheitskonzept Agent / MCP" in doc/ARCHITECTURE.md + Umsetzung im Dienst.

Doku, je Punkt: Regel | wo im Code durchgesetzt (Klasse) | Test, der es belegt
1. Autorisierung: wie Agent authentisiert wird, wie tenant_id ermittelt wird (aus P4).
2. Minimale Rechte: Scope-Liste (z. B. suggestions:read, similar:read); was explizit NICHT erlaubt ist.
3. Eingabeprüfung: Schema-Validierung der Tool-Argumente + Ownership-Check (processId gehört zum
   tenant_id des Aufrufers) – im Application-Service, nicht im Tool-Adapter.
4. Menschliche Freigabe: kritische Aktionen (DMS-Write, Promotion, Vorlagen-Consent) sind nur über
   den UI-Pfad mit Session eines Menschen auslösbar; ConsentGuard verweigert Kanal MCP.
5. Audit: jeder Tool-Call append-only, ohne Roh-PII.

Code:
- Ownership-Check zentral (z. B. in security/ConsentGuard oder eigenem ProcessAccessGuard),
  von REST und MCP gleichermassen genutzt.
- ConsentGuard: lehnt Aufrufe mit Kanal=MCP für confirm/reject/template ab.
- Ownership-Check (ProcessAccessGuard) zusätzlich für GET /api/v1/processes/{id}/events (vor Öffnen
  des Streams), GET .../similar-documents, .../document-type-candidates, .../extraction-result und
  POST .../extraction-result/retry. Fremde/unbekannte processId → 404. Test je Endpunkt.
- Audit-Datenmodell: AUDIT_ENTRY hat heute run_id als FK auf EXTRACTION_RUN. Tool-Calls ohne
  Lauf müssen auditierbar sein: run_id nullable machen und Spalten channel (UI|MCP|SYSTEM),
  actor_ref (pseudonymisiert/gehasht, keine Roh-PII), tool_name ergänzen. Flyway-Migration
  (neue Version, bestehende nicht ändern). ER-Diagramm § 8.4 nachführen.

Tests: fremder Mandant → verweigert; MCP versucht confirm-Service → verweigert + Audit-Eintrag;
Audit enthält keine Attributwerte (Assertion auf Payload).
```

---

## P6 – Erste KI-Integration nachweisen (Artefakt 6)

```text
Aufgabe: Umsetzungsstand von FR-3/FR-4 hinter EmbeddingPort/LlmPort prüfen und dokumentieren;
fehlende Teile minimal ergänzen.

1. Prüfen und im Tagebuch als Tabelle festhalten (vorhanden/fehlt/teilweise):
   LlmPort + OpenAI-kompatibler Adapter, EmbeddingPort + Adapter, serverseitige
   JSON-Schema-Validierung mit Retry gemäss docextract.adapters.llm.max-retries, "unbekannt"-Pfad,
   Audit mit Modell-ID/Token, Pre-Filter-Query (tenant_id, APPROVED, embedding_model,
   extraction_source).
2. Nur was fehlt und in SPEC/ARCHITECTURE eindeutig beschrieben ist, implementieren.
3. Tests (LLM gemockt): ungültiges JSON → Retry → kontrollierter Fehler; gültiges JSON mit Wert
   ausserhalb Werteliste → abgewiesen bzw. "unbekannt"; Cross-Tenant → 0 Treffer.
4. Contract-Test-Suite für LlmPort prüfen; falls fehlend, abstrakte Suite anlegen und vom
   vorhandenen Adapter erfüllen lassen.
5. PROJEKTPLAN.md: Status für 3.6 eintragen (nur Fakten aus Schritt 1).

Akzeptanz: Ein Dokument durchläuft im Profil ci (Mock-LLM) Upload → Vorschläge; Test belegt es.
```

---

## P7 – Repo-weite Änderung: einheitliche Fehlerform (Artefakt 7)

```text
Aufgabe: Einheitliche Fehlerform ProblemDetail (RFC 9457) für alle REST-Controller UND das MCP-Tool,
mit doc/ARCHITECTURE.md als Rahmen (§ 8.1).

1. Zentrale Fehlerklassen in shared (frameworkfrei in domain/application: eigene Exceptions
   wie LimitExceeded, NotFound, TenantMismatch, ConflictState, AdapterUnavailable).
2. Ein @ControllerAdvice in shared/adapter, der diese auf ProblemDetail abbildet:
   type-URI (Schema festlegen, z. B. urn:docextract:problem:<code>), title, status, detail ohne
   PII, processId falls vorhanden.
3. MCP-Tool: dieselben Exceptions auf ein konsistentes Tool-Fehlerformat abbilden (gleicher code).
4. Alle Controller anpassen; lokale try/catch-Fehlerantworten entfernen.
5. openapi.yaml aus P3 nachziehen.
6. Tests: je Fehlerklasse ein WebMvc-Test auf Status + type; ein Test, dass detail keine
   Attributwerte/Dateinamen enthält.
7. Abbildungen (aus P3-Befunden):
   - DmsRepositoryException, DmsObjectDefinitionException, ValueListWebhookException → 502;
     ExtractionException und Fallback → 500 ProblemDetail ohne PII.
   - Framework-Fehler via ResponseEntityExceptionHandler als ProblemDetail.
   - Datei zu gross (Multipart-Limit und Limits) → 413, gleicher type; Seitenzahl → 422;
     § 6.5 entsprechend präzisieren.
   - Range: syntaktisch ungültig → ignorieren (200, voller Inhalt); unerfüllbar → 416 mit
     Content-Range: bytes */<Länge>.

WICHTIG für die Abgabe: Erzeuge doc/block3/repo-wide-change.md mit einer Tabelle
"Datei | Änderung (1 Satz) | Entscheid: übernommen/korrigiert/verworfen | Grund | Beleg (Test/Commit)".
Spalten Entscheid und Grund LEER lassen – die füllt der Mensch nach Diff-Review.
Halte die Änderung in einem separaten Commit, damit der Diff je Datei prüfbar ist.
```

---

## P8 – Projekt-Kontext-Dokument v0.3 (Artefakt 8)

```text
Aufgabe: doc/ARCHITECTURE.md auf v0.3 heben.

1. Kopfzeile: "Version 0.3 · Stand: <heutiges Datum>" und am Ende eine Änderungshistorie:
   v0.1 (Block 1), v0.2 (Block 2: ADR-013, Frontend) – rückwirkend aus git log belegen –,
   v0.3 (Block 3: ADR-014, ADR-015, § 5.6, § 8.6, OpenAPI, MCP, Fehlerform).
2. Neuer Abschnitt "Leitplanken für Agenten" (für Coding-Agents UND den MCP-Agenten):
   - Berechtigungen: was ein Agent lesen/aufrufen darf, was nie.
   - Freigaben: welche Aktionen menschliche Freigabe brauchen (DMS-Write, Promotion,
     Vorlagen-Consent, Modellwechsel C-5, Migrationen, Änderungen an Guardrail-Code).
   - Verweis auf Service-Verträge: doc/api/openapi.yaml, MCP-Toolliste, Port-Tabelle § 5.2.
3. Falls .github/copilot-instructions.md (oder vergleichbar) existiert: Leitplanken dort
   referenzieren, nicht duplizieren.
4. PROJEKTPLAN.md: 2.6 auf ✅ und Status-Spalte für Block 3 (3.1–3.7) ergänzen, nur mit Fakten.
```

---

## P9 – Abgabe zusammenstellen

```text
Aufgabe: doc/block3/ABGABE-BLOCK3.md als Quelle für das PDF erzeugen. Nur Zusammenstellung und
Verweise, keine neuen Inhalte.

Kapitel: 1 Zerlegung (§ 5.6) · 2 TRANSFER-Entscheid (ADR-015, unverändert) · 3 OpenAPI-Auszug
(confirm-Endpunkt + Verweis auf Datei) · 4 Sicherheitskonzept (§ 8.6) · 5 MCP-Tool ·
6 KI-Integration · 7 Kommunikationsentscheid (Verweis ADR-004/ADR-015) · 8 Repo-weite Änderung
(Tabelle aus P7) · 9 KI generiert vs. korrigiert/verworfen (Auszug Tagebuch, je Stelle Diff-Link
oder Test) · Git-URL: Platzhalter <REPO-URL>.

Falls pandoc verfügbar: PDF nach doc/block3/ABGABE-BLOCK3.pdf erzeugen (Mermaid vorher als Bild
rendern, falls mermaid-cli vorhanden; sonst OFFEN markieren).
```

---

## Was du selbst tun musst (nicht an den Agenten delegieren)

- **x-slo** für `confirm` festlegen (P3)
- Agent-Authentisierung bestätigen oder ändern (P4)
- In der Tabelle der repo-weiten Änderung **Entscheid und Grund** je Datei ausfüllen (P7)
- ADR-015 inhaltlich gegenlesen: Der TRANSFER-Entscheid muss dein eigener sein
