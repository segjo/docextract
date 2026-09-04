# Copilot Instructions — DocExtract

KI-gestützte Dokumenterfassung und Verschlagwortung für **d.velop documents**.

Diese Datei steuert GitHub Copilot. Verbindliche Quellen:

1. [SPEC.md](../doc/SPEC.md)
2. [ARCHITECTURE.md](../doc/ARCHITECTURE.md)
3. diese Datei

**Bei Konflikten gilt die obige Reihenfolge. Bequemlichkeit, Framework-Defaults und generische Best Practices dürfen verbindliche Projektentscheide nicht überschreiben.**

## 1. Kontext

DocExtract liest eingehende Dokumente, schlägt strukturierte Attribute vor und schreibt diese erst nach expliziter menschlicher Freigabe ins DMS zurück. Die Anwendung läuft als iframe hinter dem d.velop Reverse Proxy. Die Authentisierung erfolgt über die d.velop-Session, ohne Impersonation. Ein Agent kann denselben fachlichen Kern über MCP verwenden. REST und MCP müssen dieselben Berechtigungs-, Consent-, Audit- und Datenschutzregeln durchlaufen.

Leitprinzipien:

- Human-in-the-Loop
- Datensouveränität by design
- Least Privilege
- Quarantine by default
- Keine Dokumentinhalte oder Embeddings ausserhalb der lokalen Betriebsgrenze

## 2. Technologieentscheidungen

### Verbindlich

- Java 21
- Spring Boot 4
- PostgreSQL mit pgvector
- modularer Monolith
- hexagonale Architektur mit Ports und Adapters
- Docker Compose
- lokale KI-Verarbeitung

### Projektentscheid, Änderung nur mit ADR

- Angular als iframe-Frontend
- docling für Parsing und HybridChunking
- Ollama als lokales Inferenz-Backend
- Gotenberg/LibreOffice für Vorschauen von Nicht-PDF-Dateien
- PostgreSQL LISTEN/NOTIFY und PROCESS_STEP für SSE-Fortschritt
- DMS-Chunk-Store für kurzlebige Binärobjekte

### Austauschbar

Konkrete Java-Client-, Mapping- und Testbibliotheken sind austauschbar, sofern Lizenzvorgaben, Constraints, Modulgrenzen und Sicherheitsanforderungen eingehalten werden.

Abweichungen von verbindlichen oder ADR-geschützten Entscheiden müssen begründet und als ADR dokumentiert werden.

## 3. Root-Package und Modulgrenzen

Root-Package: `ch.adeon.apps.docextract`

Jedes Fachmodul folgt grundsätzlich dieser Struktur:

```text
<module>
├── domain
├── application
└── adapter
    ├── in
    └── out
```

Fachmodule:

- `ingest`: Upload, Limits, Original-Chunk-Upload und Preview-Verzweigung
- `structuring`: docling-Parsing und HybridChunking
- `retrieval`: Embeddings, PENDING-Staging und ACL-gefilterte Ähnlichkeitssuche
- `extraction`: schema-validierte Attributvorschläge
- `validation`: Human-in-the-Loop, DMS-Finalisierung und Korpus-Promotion
- `agentgateway`: MCP-Inbound-Adapter
- `process`: Orchestrierung, transienter Job-Context, Prozesszustand, Lease, Recovery und SSE
- `audit`: Audit-Persistenz und technische Aufrufmetadaten
- `security`: Session-, Tenant-, ACL- und Consent-Auflösung
- `shared`: ausschliesslich technische Querschnittstypen ohne fachlichen Zustand

### Verbindliche Modulregeln

- Die Domäne kennt keine Frameworks.
- Adapter enthalten keine Fachregeln.
- Fachlogik liegt hinter Inbound-Ports und in Application-Services.
- REST und MCP verwenden dieselben Application-Services.
- Modulgrenzen sind logisch, nicht distributiv. Der Übergang `structuring → retrieval → extraction` ist ein In-Process-Aufruf.
- `shared` darf keinen `DoclingDocument`, keine Chunks, keine Retrieval-Ergebnisse und keinen vollständigen Job-Zustand enthalten.
- Der transiente Job-Context gehört dem `process`-Modul.
- Fachmodule erhalten nur unveränderliche, für den jeweiligen Aufruf erforderliche Domain-Werte oder DTOs. Sie dürfen nicht auf den vollständigen Job-Context zugreifen.
- Paketabhängigkeiten und Modulgrenzen werden mit ArchUnit geprüft.

## 4. Nicht verhandelbare Constraints

### C-1 — Betriebsgrenze und Datensouveränität

Dokumentinhalte, Chunks, Prompts, Modellkontexte und Embeddings verlassen die lokale Betriebsgrenze niemals. Ausgehende Verbindungen sind technisch auf folgende Ziele beschränkt:

- d.velop Identity Provider
- d.velop DMS-API
- konfigurierter Wertelisten-Webhook

Jeder neue Netzwerkzugriff benötigt eine explizite Architekturgrundlage. Die Egress-Allowlist wird in CI getestet.

### C-2 — Kein kritischer Schreibzugriff ohne Consent

Keine LLM-Ausgabe darf selbstständig einen DMS-Write, Tool-Call, Statuswechsel oder eine Korpus-Aktivierung auslösen.

Begriffsabgrenzung:

- Der unfinalisierte DMS-Chunk-Upload während `ingest` ist eine reversible, kurzlebige technische Übergabe und benötigt kein fachliches Consent.
- Attribut-Write, DMS-Finalisierung, fachliche Statusänderung und Korpus-Aktivierung sind kritische Aktionen und benötigen explizite menschliche Freigabe.
- Dies gilt identisch für REST und MCP.

### C-3 — Least Privilege

- Ollama besitzt keinen direkten Zugriff auf PostgreSQL, pgvector oder die DMS-API.
- Retrieval darf Dokument-Embeddings nur als PENDING mit Ablaufzeit speichern.
- Nur der consent-geschützte Validierungspfad darf PENDING-Embeddings zu APPROVED promoten.
- Der `DmsWriteAdapter` ist der einzige Pfad für die fachliche DMS-Finalisierung.
- MCP erhält nur die minimal erforderlichen Scopes.

### C-4 — Audit und Retention

Jeder LLM- und MCP-Tool-Call wird append-only protokolliert. Erfasst werden nur erforderliche technische Metadaten, zum Beispiel:

- Modell- und Prompt-Template-Version
- nicht reversibler Korrelations-HMAC oder Referenz
- Token-Verbrauch
- Konfidenz
- Ereignistyp und Zeitstempel

Audit-Einträge dürfen keine Dokumentinhalte, Chunks, Quell-Exzerpte, Attributwerte oder direkt identifizierende Personenkennungen enthalten.

`append-only` gilt während der festgelegten Aufbewahrungsdauer. Audit-Datensätze dürfen fachlich nicht aktualisiert oder überschrieben werden. Die fristgerechte Löschung erfolgt ausschliesslich über einen privilegierten und selbst auditierten Retention-Prozess, bevorzugt auf vollständigen Datensätzen oder Partitionen. Damit bleiben Unveränderlichkeit während der Aufbewahrung und NfA-5 vereinbar.

Fachlich relevante Audit-Ereignisse wie Consent, DMS-Finalisierung und Korpus-Promotion müssen explizit im Application-Service erzeugt werden. Aspekte dürfen ergänzend technische Call-Metadaten erfassen, aber keine fachlichen Audit-Ereignisse ersetzen.

### C-5 — Modell- und Supply-Chain-Integrität

Modelle und Basis-Images werden per Digest oder verifiziertem Hash gepinnt. `latest`-Tags sind im Betrieb verboten.

### C-6 — Reproduzierbarer Betrieb

`docker compose up` startet den Stack auf Linux und Windows/WSL2. CI verwendet ein LLM-Mock für Smoke- und Egress-Tests. Ein echtes Modell darf in einem getrennten Nightly-Test verwendet werden.

### C-7 — Quarantäne temporärer Embeddings

PENDING-Embeddings enthalten mindestens:

- `tenant_id`
- `acl_ref`
- `process_id`
- `attempt_id`
- `status = PENDING`
- `expires_at`
- Modell- und Provenance-Referenz

Retrieval-Abfragen müssen `status = APPROVED` sowie Tenant- und ACL-Prädikate als Pre-Filter enthalten. PENDING-Einträge dürfen auch bei maximaler Ähnlichkeit niemals als Treffer erscheinen.

Consent aktiviert vorhandene Embeddings ohne Neuberechnung. Ablehnung, endgültiger Prozessfehler oder TTL-Ablauf löscht PENDING-Einträge. `FINALIZED_INDEX_PENDING` ist ein Prozesszustand und kein Embedding-Status. Der Cleanup überspringt Embeddings, die zu einem Prozess in diesem Recovery-Zustand gehören.

## 5. Datenschutz und Speicherklassen

### Nicht persistierbar

Folgende Inhalte sind Roh-PII und dürfen niemals at-rest gespeichert werden:

- Dokumentvolltext
- `DoclingDocument`
- Chunk-Text
- kontextualisierter Chunk-Text
- Quell-Exzerpte
- vollständige LLM-Prompts mit Dokumentinhalt
- vollständige LLM-Antworten mit Dokumentinhalt

Diese Daten dürfen nur im transienten Job-Context existieren und müssen nach `extracted`, bei Abbruch und bei Fehlern verworfen werden.

### Bedingt persistierbar

Strukturierte Attributvorschläge dürfen nur gespeichert werden, soweit dies für den Validierungs-Workflow erforderlich ist. Da Attributwerte PII enthalten können, gelten zwingend:

- Tenant- und ACL-Isolation
- Datenminimierung
- definierte Retention
- Löschbarkeit gemäss NfA-5
- keine Aufnahme in technische Logs oder Audit-Payloads

Quellenbezug wird vorzugsweise ohne Text gespeichert, zum Beispiel als:

- Seitenzahl
- Bounding Box
- Chunk-Index
- DMS-Location oder Dokumentreferenz

Die UI zeigt Quell-Exzerpte nur aus dem transienten Job-Context oder rekonstruiert sie kontrolliert aus dem autorisierten DMS-Dokument. Quell-Exzerpte dürfen nicht in `ATTRIBUTE_SUGGESTION`, `PROCESS_STEP` oder `AUDIT_ENTRY` persistiert werden.

### Speicherklassen

- **PostgreSQL relational:** Metadaten, notwendige strukturierte Vorschläge, Prozesszustand, Audit-Metadaten
- **pgvector:** PENDING- und APPROVED-Embeddings ohne Chunk-Text
- **In-Memory-Job-Context:** DoclingDocument, Chunks, Quell-Exzerpte und weitere dokumentinhaltliche Zwischenprodukte
- **DMS-Chunk-Store:** Original-Bytes und bei Nicht-PDF ein kurzlebiger Preview-Chunk
- **PROCESS_STEP:** ausschliesslich PII-freier Fortschritt

## 6. Embedding-Terminologie

Die folgenden Begriffe sind verbindlich und dürfen nicht vermischt werden:

- `PendingDocumentEmbedding`: Vektor eines kontextualisierten Dokument-Chunks. Er wird mit TTL als PENDING persistiert.
- `ApprovedDocumentEmbedding`: derselbe Vektor nach menschlicher Freigabe. Die Promotion ändert Status, Provenance und Ablaufattribute, berechnet den Vektor aber nicht neu.
- `RetrievalQueryEmbedding`: nur falls die Retrieval-Strategie einen separaten Suchvektor erzeugt. Er bleibt transient und wird niemals persistiert.

Ein PENDING-Dokument-Embedding darf nicht als transienter Query-Vektor bezeichnet werden.

Das Embedding- und das Extraktionsmodell sind getrennt konfiguriert:

```text
docextract.ai.embedding.model
docextract.ai.embedding.tokenizer
docextract.ai.embedding.max-tokens
docextract.ai.extraction.model
docextract.ai.extraction.context-window
```

Tokenizer und `max_tokens` des Chunkers müssen zum Embedding-Modell passen. Sie dürfen nicht implizit aus dem Extraktionsmodell abgeleitet werden.

## 7. Datenfluss

### Happy Path

1. Session validieren und Tenant-/ACL-Kontext ableiten.
2. Upload-Limits prüfen.
3. Original synchron als unfinalisierten DMS-Chunk streamen.
4. DMS-Location, PROCESS und initialen PROCESS_STEP dauerhaft speichern.
5. Erst nach erfolgreichem Commit den asynchronen Job planbar machen.
6. `202 Accepted` mit `processId` zurückgeben.
7. Native PDFs direkt aus der DMS-Location streamen.
8. Nicht-PDFs mit Gotenberg rendern und als separaten Preview-Chunk speichern.
9. docling erzeugt ein DoclingDocument und kontextualisierte HybridChunks.
10. Chunks im transienten Job-Context halten.
11. Retrieval erzeugt Dokument-Embeddings und speichert sie als PENDING.
12. ANN-Suche nur gegen APPROVED mit Tenant-/ACL-Pre-Filter durchführen.
13. Extraction erzeugt schema-validierte Attributvorschläge.
14. PII-freien Fortschritt publizieren.
15. Nach `extracted` den Job-Context vollständig verwerfen.

### Async-Transaktionsregel

- Der Job darf erst nach erfolgreichem Commit von DMS-Location, PROCESS und initialem PROCESS_STEP gestartet oder von einem Worker geclaimt werden.
- Keine `@Async`-Self-Invocation innerhalb derselben Spring-Bean.
- Verwende einen After-Commit-Mechanismus oder einen dauerhaft gespeicherten, atomar claimbaren Prozesszustand.
- `202 Accepted` darf erst zurückgegeben werden, wenn Original-Location und Prozessdatensatz dauerhaft gespeichert sind.
- Der Async-Start muss idempotent sein.

## 8. Recovery und Idempotenz

### Job-Recovery

- Bei Instanzausfall werden `DoclingDocument` und Chunks aus der autorisierten DMS-Location neu erzeugt. Sie werden niemals aus PostgreSQL rekonstruiert.
- Jeder Verarbeitungsversuch besitzt eine eindeutige `attempt_id`.
- PENDING-Embeddings müssen über `process_id + attempt_id + chunk_index` idempotent upsertbar sein oder vor dem Retry vollständig und kontrolliert ersetzt werden.
- Vorhandene APPROVED-Embeddings dürfen durch Recovery niemals verändert oder gelöscht werden.
- Die maximale Anzahl Wiederholungen und Stufen-Timeouts sind konfiguriert.
- Nach Ausschöpfen der Wiederholungen endet der Prozess in einem definierten Fehlerzustand und verbleibende PENDING-Einträge werden gelöscht.

### Validierungs-Saga

DMS-Finalisierung und PostgreSQL-Promotion bilden keine gemeinsame ACID-Transaktion.

Verbindliche Reihenfolge:

1. Consent und Identität prüfen.
2. gültige PENDING-Embeddings sperren und Tenant, Prozess und Ablauf prüfen.
3. DMS-Dokument idempotent finalisieren.
4. Embeddings in PostgreSQL atomar von PENDING zu APPROVED promoten.
5. Provenance setzen und `expires_at` entfernen.
6. fachliche Audit-Ereignisse schreiben.

Falls Schritt 4 fehlschlägt, wechselt der Prozess zu `FINALIZED_INDEX_PENDING`. Ein Retry wiederholt nur die idempotente Promotion und nicht die fachliche DMS-Finalisierung.

„Atomar“ bezeichnet ausschliesslich die PostgreSQL-Promotion. Die gesamte DMS-/DB-Abfolge ist eine Saga und nicht atomar.

## 9. Sicherheits- und KI-Guardrails

- Dokumentinhalt ist immer untrusted data und niemals Systeminstruktion.
- Schema-constrained Decoding und JSON-Schema-Validierung sind verpflichtend.
- Aus Dokumentinhalt dürfen keine Tool-Calls oder Schreibaktionen abgeleitet werden.
- Tenant- und ACL-Prädikate stammen ausschliesslich aus dem serverseitig validierten AuthContext.
- Tenant-, ACL- und `status = APPROVED`-Filter sind Teil der Vektor-Query, niemals nachgelagerte Post-Filter.
- Harte Limits gelten für Dateigrösse, Seitenzahl, Laufzeit, Parallelität, Chunk-Anzahl und Modellkontext.
- MCP umgeht keine UI-Guardrails und erhält keine privilegierte Abkürzung.
- Bei Zielkonflikten haben C-1 und NfA-5 Vorrang.

In Scope ist eine technisch testbare Tenant-/ACL-Isolation im Retrieval. Out of Scope ist eine vollständige produktive Multi-Tenancy-Plattform mit Tenant-Provisioning, Abrechnung, Administration und Lifecycle-Management.

## 10. Spring-Boot-Konventionen

- Constructor Injection statt Field Injection
- zentrale Fehlerbehandlung mit `@ControllerAdvice`
- Fehlerantworten als `ProblemDetail` nach RFC 9457
- OpenAPI-spezifizierte REST-Endpunkte
- Konfiguration über `application.yml` und Profile `local`, `ci`, `cluster`
- Secrets niemals im Image oder Repository
- jede Pipeline-Stufe mit eigenem Timeout
- fachliche Transaktionen in Application-Services
- keine Persistenzzugriffe aus Domain-Klassen
- keine fachliche Logik in Controllern oder Adapters
- strukturierte Logs ohne Dokumentinhalt, Chunk-Text, Prompts, Attributwerte oder Quell-Exzerpte

## 11. Test- und CI-Erwartungen

Für jede signifikante Änderung sind passende Tests zu erzeugen oder vorzuschlagen:

- Unit-Tests für Domänenlogik, Limits, Schema-Validierung und ACL-Prädikate
- Integrationstests für pgvector, docling, Ollama-Mock und DMS-Chunk-Adapter
- Cross-Tenant-/Cross-ACL-Test mit exakt null Fremdtreffern
- Test, dass PENDING niemals im Retrieval erscheint
- Test der Promotion ohne erneute Embedding-Berechnung
- Retention- und Rest-PII-Test
- Test, dass keine Chunks oder Quell-Exzerpte at-rest landen
- Fehlerinjektion für ungültiges LLM-JSON, docling-Absturz und OCR-Müll
- SSE-Reconnect und PROCESS_STEP-Catch-up
- Lease-, Recovery-, Attempt- und Idempotenztests
- Preview-Branch: PDF ohne Gotenberg, Nicht-PDF mit Preview-Chunk
- ArchUnit-Tests für Paket- und Modulgrenzen
- Egress-Allowlist-Test

CI-Gate vor Publish:

- Build und Unit-Tests
- Integration- und Smoke-Test
- Egress-Test
- SAST
- Dependency-Scan
- Secret-Scan
- Image-Scan
- IaC-/Compose-Scan

## 12. Do and Don't

### DO

- Fachlogik hinter Inbound-Ports implementieren.
- REST und MCP auf dieselben Application-Services führen.
- Berechtigungen aus dem serverseitigen AuthContext beziehen.
- ACL und Tenant direkt in der Vektor-Query filtern.
- fachliche Audit-Ereignisse explizit erzeugen.
- Recovery und Promotion idempotent implementieren.
- Annahmen sichtbar dokumentieren.

### DON'T

- keinen Dokumentvolltext, Chunk-Text oder Quell-Exzerpt persistieren
- keine Roh-PII loggen oder auditieren
- keinen DMS-Finalisierungs- oder Korpus-Write ohne Consent ausführen
- Ollama keinen DB- oder DMS-Zugriff geben
- keinen Retrieval-Post-Filter als Ersatz für den Pre-Filter verwenden
- keine PENDING-Embeddings abrufbar machen
- Embeddings bei Freigabe nicht neu berechnen
- keinen Postgres-Blobstore oder externen Message-Broker ergänzen
- keinen Microservice-Split ohne begründende NfA und ADR vornehmen
- keinen neuen Egress-Zugriff ohne Architekturgrundlage ergänzen
- keinen fachlichen Zustand in `shared` ablegen

## 13. Vorgehen bei Unklarheiten

- Keine Architekturentscheidung stillschweigend treffen.
- Zuerst SPEC.md und ARCHITECTURE.md heranziehen.
- Wenn dort keine eindeutige Regel existiert, die sichere und datensparsame Variante wählen und die Annahme sichtbar kennzeichnen.
- Ein ADR vorschlagen, sobald Persistenz, Netzwerkzugriff, Modulgrenze, Consent, Berechtigung, Hintergrundverarbeitung, KI-Tool-Aktion oder Betriebsmodell betroffen ist.
- Keine neue Persistenz, externe Verbindung, Hintergrundverarbeitung oder privilegierte Aktion ohne explizite Architekturgrundlage ergänzen.

_Stand: 09/2026 · lebendes Dokument, versioniert mit SPEC.md und ARCHITECTURE.md._
