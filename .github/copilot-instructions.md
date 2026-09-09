# Copilot Instructions — DocExtract

> Verbindliche Arbeitsanweisungen für GitHub Copilot / Copilot Chat in diesem Repository.
> Quelle der Wahrheit: [`SPEC.md`](SPEC.md) (Anforderungen) und [`ARCHITECTURE.md`](ARCHITECTURE.md) (arc42/C4 + ADRs).
> Externe API-Verträge: [`doc/api/dvelop-dmsapp.yaml`](doc/api/dvelop-dmsapp.yaml) (DMS),
> [`doc/api/identityprovider-api.yaml`](doc/api/identityprovider-api.yaml) (IdP-Session-Validierung) und
> [`doc/api/dashboard-app-api.yaml`](doc/api/dashboard-app-api.yaml) (d.velop Dashboard-App-Integration) —
> vor jeder Adapter-Implementierung gegen diese OpenAPI-Specs prüfen, nicht aus dem Gedächtnis raten.
> Bei Konflikt gilt: **SPEC/ARCHITECTURE > diese Datei > Framework-Defaults > generische Best Practices.**
> Diese Datei ersetzt keine Anforderung; sie operationalisiert sie.

---

## 1. Projekt in einem Satz

DocExtract erfasst und verschlagwortet eingehende Dokumente **ohne manuelles Abtippen** und legt sie
**erst nach menschlicher Freigabe** im DMS (d.velop documents) ab — schnell, nachvollziehbar,
**datensouverän**. Die App läuft als **iframe hinter dem d.velop Reverse Proxy**; ein Agent nutzt denselben
Kern über ein **MCP-Tool** mit identischen Guardrails.

**Leitprinzipien, die in jeder Antwort mitgedacht werden:**
`Human-in-the-Loop` · `Datensouveränität by design` · `Least Privilege` · `Datenminimierung` · `Nachvollziehbarkeit`.

---

## 2. Technischer Rahmen (Randbedingungen — nicht eigenmächtig ändern)

| Bereich                      | Vorgabe                                                                             |
| ---------------------------- | ----------------------------------------------------------------------------------- |
| **Sprache/Runtime**          | Java 21                                                                             |
| **Backend**                  | Spring Boot 4 (hexagonaler Kern, DI, `@ControllerAdvice`, `ProblemDetail`/RFC 9457) |
| **Frontend**                 | Angular (als iframe), Upload · PDF-Vorschau · Validierungs-UI                       |
| **Strukturierung**           | docling (Container) → `DoclingDocument` → **HybridChunker**                         |
| **KI-Inferenz**              | Ollama, lokal (Qwen 3): **getrennt** Embedding-Modell + LLM                         |
| **Vektor/Persistenz**        | PostgreSQL + pgvector (Metadaten, Korpus-Vektoren, Audit, Process-State)            |
| **Vorschau (nur Nicht-PDF)** | Gotenberg/LibreOffice                                                               |
| **Betrieb**                  | Docker Compose, reproduzierbar auf **Linux und Windows/WSL2**, **Cluster-Modus**    |
| **Observability**            | Micrometer/Prometheus, OpenTelemetry, strukturiertes Logging je Pipeline-Stufe      |

> Der Stack ist für dieses Repository gemäss den akzeptierten Architekturentscheiden verbindlich. **Keine** Ersatz-Bibliotheken,
> **keine** zusätzlichen Infrastrukturdienste (z. B. Kafka/Rabbit, S3/MinIO) vorschlagen — siehe ADR-001/-004/-005.

---

## 3. Nicht verhandelbare Regeln (Guardrails)

Diese Regeln haben **immer** Vorrang. Code, der dagegen verstößt, ist nicht akzeptabel.

### 3.1 Constraints (dauerhaft gültig)

- **C-1 Datensouveränität:** Dokumentinhalte, Chunks, Prompts, LLM-Kontexte und Embeddings verlassen die
  lokale Betriebsgrenze **nie**. Egress ist per Netzwerk-Policy unterbunden und in CI per **Egress-Test** verifiziert.
  Nach außen fließen **ausschließlich** Metadaten-Lese/-Schreibzugriffe auf die DMS-API, IdP-Session-Validierung
  und JIT-Wertelisten-Abrufe. Original-/Preview-Bytes dürfen **nur** an die autorisierte DMS-API gehen.
- **C-2 Kein automatischer Schreibzugriff:** Keine LLM-Ausgabe löst selbsttätig einen Schreib-/Tool-Call aus.
  DMS-Write **nur nach expliziter menschlicher Freigabe** (Consent-Gate) — UI **und** MCP identisch.
- **C-3 Least Privilege:** Der LLM-/Inferenz-Service hat **keinen** direkten Zugriff auf PostgreSQL/pgvector oder
  die DMS-API. Der Retrieval-Adapter darf Embeddings **nur als `PENDING` mit TTL** schreiben. Nur der
  Consent-geschützte Validierungspfad darf `PENDING → APPROVED` setzen. MCP-Tool erhält minimale Scopes.
- **C-4 Unveränderlicher Audit-Pfad:** Jeder LLM-/Tool-Call wird **append-only** protokolliert
  (Modellversion, Prompt-Hash, **Token-Verbrauch**, Konfidenz) — **ohne roh-PII** (nur Hashes/Referenzen).
- **C-5 Modell-Integrität:** Modelle per **Hash/Digest gepinnt**; nur verifizierte Artefakte in Betrieb.
- **C-6 Reproduzierbarer Betrieb:** `docker compose up` auf Linux und Windows/WSL2; Smoke-Test in CI (LLM als Mock).
- **C-7 Quarantäne temporärer Embeddings:** `PENDING`-Vektoren tragen `tenant_id`, `acl_scope`, `process_id`,
  `expires_at`. Retrieval berücksichtigt **ausschließlich `status = APPROVED`**. Freigabe aktiviert vorhandene
  Vektoren **ohne Neuberechnung**. Ablehnung/Prozessfehler/TTL-Ablauf löschen `PENDING`. Recovery-Zustand
  `FINALIZED_INDEX_PENDING` ist bis Promotion-Abschluss vor Cleanup geschützt.

### 3.2 Bedrohungen → Pflicht-Mitigationen

- **T-1 Prompt Injection:** Dokument strikt als _untrusted data_, **nie** als Instruktion/System-Prompt;
  **schema-constrained Decoding** + Output-Validierung gegen JSON-Schema; keine aus Inhalt ableitbaren Tool-Calls.
- **T-2 Datenvergiftung (Self-Learning):** Embeddings vor Freigabe nur als **nicht-retrievalfähige `PENDING`**
  mit TTL; Aktivierung erst nach Consent; Herkunfts-/Quarantäne-Markierung; Rollback unterstützen.
- **T-3 Cross-Tenant-/Cross-ACL-Leak:** **Pre-Filter** (Tenant- und ACL-Prädikate) _in_ die Vektor-Query,
  **niemals** Post-Filter. Ziel: **0 Fremdtreffer**.
- **T-4 DoS:** Harte Limits (Dateigröße, Seitenzahl, Timeouts je Stufe) + kontrollierter Abbruch.
- **T-5 Modellmanipulation:** Digest-Pinning + Integritätsprüfung vor Deployment.
- **T-6 MCP-Missbrauch:** Minimale Scopes; **Consent vor kritischen Aktionen**; kein DMS-Write ohne Freigabe;
  jeder Tool-Call append-only auditiert.

---

## 4. Architektur-Leitplanken

- **Modularer Monolith** (ADR-001): **kein** Microservice-Split. Cluster über **gemeinsame Datenhaltung** +
  instanzgebundene, transiente Jobs — nicht über Service-Zerlegung. Kein Netz-Hop zwischen Fachmodulen.
- **Hexagonal (Ports & Adapters):** Domäne kennt keine Frameworks; Adapter kennen keine Fachregeln.
  Querschnitte (`security`, `audit`) werden über DI/Aspekte an **jedem** Inbound-Pfad (REST **und** MCP) eingezogen.
- **Adapter-Symmetrie:** REST-Adapter und MCP-Adapter teilen **denselben** Application-Service → Guardrails
  gelten für beide Pfade automatisch.
- **Modultrennung ist logisch, nicht distributiv:** per **ArchUnit** erzwungen; ein Dokument-Job läuft
  Ende-zu-Ende auf **einer** Instanz.
- **Async + Fortschritt (ADR-004):** Nicht-blockierender Upload (`202 + processId`), **SSE** je `processId`,
  cluster-weiter Fan-out via **Postgres LISTEN/NOTIFY**, Catch-up aus durable **PII-freier** `PROCESS_STEP`.
  **Kein externer Broker.** SSE-Payload trägt nur `processId`/`step` — nie PII.
- **Kurzlebige Binär-Objekte (ADR-005):** **DMS-Chunk-Store** als Ephemeral-Store. Native **PDFs** direkt aus der
  DMS-Location streamen (kein Gotenberg-Render); **Nicht-PDFs** via Gotenberg → separater, unfinalisierter
  Preview-Chunk. **Kein Postgres-Blobstore.**
- **Datenminimierung (ADR-006):** **Chunks und `DoclingDocument` bleiben ausschliesslich in-memory** und werden nach
  `extracted` verworfen. Rohtext wird **nie** persistiert. Dokument-Embeddings werden bis zur Freigabe als nicht
  retrievalfähige `PENDING`-Einträge mit TTL gespeichert; dauerhaft bleiben nur freigegebene Korpus-Embeddings,
  Metadaten und Audit-Hashes.
- **docling-natives Chunking (ADR-007):** **HybridChunker auf dem `DoclingDocument`** (struktur- + token-basiert,
  `contextualize()` mit Überschriften-Kontext) — **kein** Post-Export-Markdown-Splitting. Die **ChunkingConfig
  (Tokenizer, `max_tokens`) liefert das `retrieval`-Modul**, weil Chunk-Größe eine Eigenschaft des Embedding-Modells
  ist (Tokenizer-Alignment).

---

## 5. Paket- & Modulstruktur (verbindlich)

Basis-Package: **`ch.adeon.apps.docextract`**

```
ch.adeon.apps.docextract
├─ ingest         # Upload, Limits, synchroner Original-Chunk-Upload; Preview nur bei Nicht-PDF (FR-1)
├─ structuring    # docling → kontextualisierte Chunks (in-memory, KEIN Persist) (FR-2, ADR-006/-007)
├─ retrieval      # Embedding, PENDING-Staging (TTL), ANN-Suche nur APPROVED + ACL-Pre-Filter (FR-3, NfA-4/-6, C-7)
├─ extraction     # schema-constrained LLM-Attributvorschläge + Quell-Exzerpte (FR-4, T-1)
├─ validation     # Human-in-the-Loop, DMS-Write, atomare PENDING→APPROVED-Promotion (FR-5, C-2, C-7)
├─ agentgateway   # MCP-Tool, teilt Application-Services (FR-6, T-6)
├─ process        # Async-Orchestrierung, SSE, PII-freier Step-State, Lease/Heartbeat/Recovery (ADR-004)
├─ audit          # append-only, ohne roh-PII, Token-/Kosten-Erfassung (C-4, NfA-7)
├─ security       # Session→Tenant/ACL-Auflösung, Consent-Gate
└─ shared         # Fehlerbehandlung, Config, Observability, In-Memory-Job-Context
```

**Modul-Regeln, die Copilot einhalten muss:**

- Jedes Fachmodul folgt `domain` / `application` / `adapter(.in|.out)`.
- **`domain` importiert keine Framework-Klassen** (kein Spring in der Domäne).
- Chunking **ausführen** in `structuring`; Chunking-**Config** kommt aus `retrieval` (dünne Config-Kopplung).
- **Ein einziger Schreibpfad ins DMS:** `DmsWriteAdapter`, separat authentifiziert, nur hinter Consent-Gate.
- Neue Cross-Module-Aufrufe nur über definierte **Ports** — keine direkten Adapter-zu-Adapter-Zugriffe.

---

## 6. Datenmodell & Persistenz-Regeln

- Rohtext/Chunks **niemals** in Tabellen oder Objektspeicher schreiben.
- `EMBEDDING` speichert: Vektor, `document_id`, `chunk_index`, `tenant_id`, `acl_ref`, `status`
  (`PENDING|APPROVED`), `process_id`, `expires_at`, `approved_at`, `provenance` — **kein** Roh-Text.
- pgvector-Index (HNSW/IVFFlat) muss **filterfähig** sein (`tenant_id`/`acl_ref` als Query-Prädikat, Pre-Filter).
- Migrationen versioniert (Flyway/Liquibase).
- Löschpfad (NfA-5) kaskadiert Dokument + Embeddings; Audit bleibt referenzierend (Hashes) erhalten.
- **DMS-Write + pgvector-Promotion sind keine gemeinsame ACID-Transaktion** → idempotente Saga:
  1. DMS finalisieren, 2) Embeddings promoten. Bei Fehler: `FINALIZED_INDEX_PENDING`; Retry führt nur die
     idempotente Promotion erneut aus; TTL-Cleanup überspringt diesen Zustand.

---

## 7. Coding-Konventionen

### Java

- **google-java-format** ist verbindlicher Formatter (im Repo gepinnt via `GOOGLE_JAVA_FORMAT_VERSION`);
  IntelliJ nutzt das google-java-format-Plugin, VS Code die entsprechende Integration. Keine abweichende Formatierung.
- Fehlerfälle über `@ControllerAdvice` zentral; nach außen als `ProblemDetail` (RFC 9457).
- Konfiguration in `application.yml` + Profile (`local`, `ci`, `cluster`); **Secrets nie im Image**.
- Jede Pipeline-Stufe hat einen eigenen **Timeout** und definierten Abbruch.

### Frontend / weitere Dateien

- **Prettier** als Formatter (Config im Repo eingecheckt, damit VS Code und IntelliJ identisch formatieren);
  ESLint/Angular-Lint als **Linter** (Lint ≠ Format nicht vermischen).
- Formatierungs-Check läuft als eigener, schneller **CI-Schritt** (nur geänderte Dateien im PR, ganzes Repo im Main).

### Allgemein

- Diffs klein und nachvollziehbar halten; Namen/Verträge stabil.
- Kommentare erklären **warum** (Bezug auf FR/NfA/ADR), nicht das Offensichtliche.

---

## 8. Teststrategie (Erwartung an generierten Code)

Zu jedem nicht-trivialen Beitrag gehören passende Tests:

| Stufe                 | Fokus                                                                                           |
| --------------------- | ----------------------------------------------------------------------------------------------- |
| **Unit**              | Domänenlogik: Limits, Schema-Validierung, ACL-Prädikat, ChunkingConfig-Ableitung                |
| **Integration**       | Adapter: pgvector, docling HybridChunker, **Ollama als Mock**, DMS-Chunk                        |
| **Chunking**          | Tokenizer-Alignment (`max_tokens` ↔ Embedding-Modell); Kontextualisierung enthält Überschriften |
| **Fehlerinjektion**   | ungültiges LLM-JSON, docling-Absturz, OCR-Müll → sauberer `failed`-Endzustand (NfA-3)           |
| **Cross-Tenant/ACL**  | automatisierter Zugriffstest → **0 Fremdtreffer** (NfA-4)                                       |
| **Async/SSE**         | Events vollständig & geordnet; Reconnect-Catch-up; abgelaufene Lease sauber beendet             |
| **Datenminimierung**  | nach `extracted` keine Chunks/Rohtexte at-rest; Embeddings nur `PENDING` mit TTL                |
| **Korpus-Quarantäne** | Retrieval liefert **nie** `PENDING`; Consent promotet ohne Neuberechnung                        |
| **Architektur**       | **ArchUnit**: Paketabhängigkeiten/Modulgrenzen                                                  |

**CI-Gate:** Smoke- + Egress-Allowlist-Test (LLM als Mock) + Security-Scan (SAST/Dependency/Secret/Image/IaC).
Publish erst nach bestandenem Gate.

---

## 9. Qualitätsziele als Akzeptanz-Hebel (SMART, SPEC § 3)

Vorschläge müssen diese Ziele unterstützen, nicht untergraben:

- **NfA-1 Extraktionsgüte:** kritische Felder ≥ 98 %, übrige ≥ 85 %; E-5 = korrekte Ablehnung („unbekannt“ statt Halluzination).
- **NfA-2 Effizienz:** p95 Upload→Vorschlag ≤ 30 s (E-1..E-3) / ≤ 60 s (E-4) bei ≤ 16 GB Peak-RAM.
- **NfA-3 Zuverlässigkeit:** 100 % Läufe in gültigem Endzustand (auch unter Fehlerinjektion).
- **NfA-4 Isolation:** 0 unberechtigte Fremdtreffer (Tenant **und** ACL).
- **NfA-5 Datenschutz:** 100 % der Löschanforderungen, keine Rest-PII.
- **NfA-6 Retrieval-Güte:** Precision@3 ≥ 0.66.
- **NfA-7 Kosten:** ≤ 8 000 Token/Dok (Median), Ausreißer > 2× begründet.

---

## 10. Verbindliche Grundregeln (Kurzform für jede Antwort)

1. **Mensch entscheidet:** keine irreversible Aktion (DMS-Write, Statuswechsel, Vorlagen-Aufnahme) ohne Consent — auch nicht via MCP.
2. **Least Privilege überall:** LLM liest nur; Schreiben über separaten, authentifizierten Service.
3. **Datensouveränität by design:** Egress der Verarbeitungs-Container technisch unterbunden und in CI getestet.
4. **Vollständige Nachvollziehbarkeit:** jeder LLM-/Tool-Call append-only, ohne roh-PII.
5. **Datenschutz vor Komfort:** bei Zielkonflikt haben C-1 und NfA-5 Vorrang.
6. **Bequemlichkeit, Framework-Defaults und generische Best Practices dürfen verbindliche Projektentscheide nicht überschreiben.**

---

## 11. Wenn Copilot unsicher ist

- Bei Widerspruch zwischen Anfrage und SPEC/ARCHITECTURE: **auf den Konflikt hinweisen** und die dokumentierte
  Entscheidung (ADR/FR/NfA/C/T) referenzieren, statt sie stillschweigend zu überschreiben.
- Fehlt eine Entscheidung, ist sie **offen** (siehe SPEC „Offen gelassen“ / weitere ADRs pro Block) → als Annahme
  kennzeichnen, nicht als Fakt.
- Änderungen an Architektur-Grundentscheiden gehören in einen **ADR**, nicht in beiläufigen Code.
