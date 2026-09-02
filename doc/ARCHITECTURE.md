# ARCHITECTURE — DocExtract

**KI-gestützte Dokumenterfassung & -verschlagwortung für d.velop documents**
Architektur-Kontext-Dokument (KI-Rahmen) · Gliederung in Anlehnung an **arc42** · Diagramme in **C4 (Mermaid)**
Bezugsdokument: [SPEC.md](SPEC.md) · Blockplanung: [PROJEKTPLAN.md](PROJEKTPLAN.md) · Entscheidungen: § 9 (ADRs)
**Stand:** 09/2026 · **Status:** lebendes Dokument, versioniert pro Block (v0.1 → v1.0)

---

## 0. Wie dieses Dokument das Bewertungsraster bedient (Traceability)

Dieses Kapitel ist die Landkarte für Korrektur und Präsentation. Es bildet die relevanten Rasterkriterien auf die Abschnitte ab, damit jede geforderte Perspektive nachweisbar ist.

| Rasterkriterium (Gruppe)                                             | Erwartung                             | Abschnitt in diesem Dokument                                           |
| -------------------------------------------------------------------- | ------------------------------------- | ---------------------------------------------------------------------- |
| **Entwurf (4)** Lösungsansatz/Architektur bildlich + textuell        | C4-Sichten + Prosa                    | § 3 (Kontext L1), § 5 (Container L2 + Paketstruktur), § 7 (Deployment) |
| **Entwurf (5)** Perspektiven Struktur · Verhalten · Interaktion      | Alle drei sichtbar                    | § 5 (Struktur), § 6 (Verhalten/Runtime), § 3 (Interaktion/Kontext)     |
| **Entwurf (6)** Datenmodell spezifiziert                             | ER-/Schemamodell                      | § 8.4 (Datenmodell + Migrationen)                                      |
| **Programmierung (7)** Schichten/Module, Verantwortlichkeiten        | Hexagonal + Modulschnitt              | § 4, § 5.2, § 5.3                                                      |
| **Programmierung (8)** Framework-Konzepte (DI, REST, Config, Fehler) | Spring-Boot-Idiome                    | § 8.1–8.3                                                              |
| **Validierung (12)** Test-/Sicherheitsstrategie                      | Teststufen + Threat-Mitigation        | § 10                                                                   |
| **KI & Architektur (16)** substanzielle KI-Funktion abgesichert      | Retrieval + Extraktion + Guardrails   | § 6.2, § 8.5, § 10.3                                                   |
| **KI & Architektur (17)** Modulgrenzen, containerlauffähig           | Modularer Monolith, Compose           | § 4, § 5, § 7                                                          |
| **KI & Architektur (18)** Reflexion/Veto                             | 3 bewusst nicht delegierte Entscheide | § 9 (ADRs) + § 11                                                      |

_Grundlage: Bewertungsraster (18 Kriterien / 100 Punkte) und Aufgabenstellung Projektarbeit (arc42 empfohlen, C4 L1/L2 Pflicht, ADRs für die wichtigsten Entscheidungen)._

---

## 1. Einführung und Ziele (arc42 §1)

DocExtract erfasst und verschlagwortet eingehende Dokumente **ohne manuelles Abtippen** und legt sie nach menschlicher Freigabe im DMS (d.velop documents) ab — schnell, nachvollziehbar, datensouverän. Fachliche Vision, Stakeholder und Kernfunktionen sind in [SPEC.md § 1 / § 5.1](SPEC.md) definiert; dieses Dokument beschreibt **wie** die Lösung strukturell, verhaltensseitig und im Betrieb aufgebaut ist.

### 1.1 Wichtigste Qualitätsziele (verkürzt, Details SPEC § 3)

| Prio | Qualitätsziel                             | Architektonischer Treiber                                            |
| ---- | ----------------------------------------- | -------------------------------------------------------------------- |
| 1    | **Datensouveränität** (NfA-5, C-1)        | Alle Verarbeitung innerhalb der Vertrauensgrenze; Egress unterbunden |
| 2    | **Mandanten-/ACL-Isolation** (NfA-4, T-3) | Berechtigungs-Pre-Filter im Retrieval, aus Session abgeleitet        |
| 3    | **Extraktionsgüte** (NfA-1)               | Schema-constrained Decoding, Retrieval-gestützte Prompts             |
| 4    | **Effizienz p95** (NfA-2)                 | Pipeline mit Stufen-Timeouts, cluster-fähiger, zustandsloser Kern    |
| 5    | **Nachvollziehbarkeit** (C-4)             | Append-only Audit-Log ohne roh-PII                                   |

### 1.2 Architektonisch relevante Stakeholder

Sachbearbeiter:in (UI-Pfad), Agent (MCP-Pfad), Records-/DMS-Owner, IT-Betrieb/Datenschutz, d.velop als Plattform — Interessen siehe [SPEC.md § 1](SPEC.md).

---

## 2. Randbedingungen (arc42 §2)

| Typ         | Randbedingung                                                                                                                               | Quelle                     |
| ----------- | ------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------- |
| Technisch   | Java 21 · Spring Boot 4 · Angular (iframe) · pgvector/PostgreSQL · docling · Ollama · Docker Compose                                        | SPEC § 4 (Empfehlung)      |
| Integration | App läuft **als iframe hinter d.velop Reverse Proxy** auf lokalem HTTP-Endpunkt; AuthN via **d.velop-Cookie/Session** (keine Impersonation) | SPEC § 4                   |
| Betrieb     | Reproduzierbar per `docker compose up` auf Linux **und** Windows/WSL2; **Cluster-Modus** (mehrere Instanzen, gemeinsame Datenhaltung)       | SPEC § 5.2 C-6             |
| Sicherheit  | Egress technisch unterbunden + CI-Egress-Test; Least Privilege; Digest-Pinning; append-only Audit                                           | SPEC § 5.2 C-1/C-3/C-4/C-5 |
| Prozess     | Projekt-Kontext-Dokument versioniert; ADRs für Grundentscheide; arc42 empfohlen                                                             | Projektarbeit Block 1–5    |

---

## 3. Kontextabgrenzung — C4 Level 1 (arc42 §3 / Perspektive: Interaktion)

**Textuell:** Die Sachbearbeiter:in bedient DocExtract ausschliesslich im Browser über das **d.velop-Frontend**, das die App als iframe einbettet; der **Reverse Proxy** routet auf den lokalen HTTP-Endpunkt. Ein zweiter Konsument, ein **Agent**, nutzt denselben Kern über ein **MCP-Tool** — mit identischen Guardrails. Nach aussen fliessen **nur** Metadaten-Lese-/Schreibzugriffe auf die DMS-API sowie JIT-Wertelisten-Abrufe; Dokumentinhalte und Embeddings verlassen die Vertrauensgrenze nie (C-1).

```mermaid
C4Context
    title C4 L1 — Systemkontext DocExtract
    Person(sb, "Sachbearbeiter:in", "Erfasst & verschlagwortet Dokumente im Browser")
    System_Ext(agent, "Agent (Redmine-/CI-Assistent)", "Konsumiert Extraktion via MCP")

    Enterprise_Boundary(dv, "d.velop Plattform") {
        System_Ext(dvfe, "d.velop Frontend + Reverse Proxy", "Bettet DocExtract als iframe ein, routet HTTP")
        System_Ext(idp, "d.velop Identity Provider", "Cookie-basierte AuthN, Tenant/ACL")
        System_Ext(dms, "d.velop DMS-API", "Dokumente hochladen / Metadaten lesen / bestätigte Attribute zurückschreiben")
    }

    System_Boundary(tb, "Vertrauensgrenze (lokale Betriebsgrenze, kein Egress)") {
        System(docx, "DocExtract", "Extraktion, Retrieval, Validierung, Rückschreiben")
    }

    System_Ext(tpa, "Third-Party-App (optional)", "Wertelisten per JIT-Webhook")

    UpdateElementStyle(tb, $borderColor="red")

    Rel(sb, dvfe, "bedient (HTTPS)")
    Rel(dvfe, docx, "routet iframe-Requests + Session-Cookie", "HTTP localhost")
    Rel(agent, docx, "ruft Extraktion/Retrieval", "MCP")
    Rel(docx, idp, "validiert Session, leitet Tenant/ACL ab")
    Rel(docx, dms, "lädt Dokument als Chunk hoch (Location) / liest ähnliche Metadaten / schreibt Attribute nach Freigabe an Location")
    Rel(docx, tpa, "holt Wertelisten (JIT)")
```

> **Vertrauensgrenze (rot):** Alles innerhalb `System_Boundary tb` — docling, Embeddings, Ollama-Inferenz, pgvector — bleibt lokal. Kontrollierter Ausgang nur zu IdP, DMS-API und Wertelisten-Webhook.

### 3.1 Externe Schnittstellen

| Nachbarsystem          | Richtung | Protokoll     | Zweck                                                                                                                                                      | Sicherheitsnote                                                                                     |
| ---------------------- | -------- | ------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------- |
| d.velop Frontend/Proxy | in       | HTTP (iframe) | UI-Auslieferung + REST                                                                                                                                     | Session-Cookie durchgereicht                                                                        |
| d.velop IdP            | out      | HTTP          | Session → Tenant/ACL-Prädikate                                                                                                                             | Basis für NfA-4                                                                                     |
| d.velop DMS-API        | in/out   | HTTP          | Zweiphasig: (1) Dokument-Chunk hochladen → `Location` im Response-Header, (2) nach Freigabe Attribute an diese `Location` schreiben (finalisiert Dokument) | Finaler Write **nur** nach Consent (C-2); `Location` wird bis zur Freigabe serverseitig vorgehalten |
| Third-Party-App        | out      | Webhook (JIT) | Wertelisten                                                                                                                                                | Client-only, kein Import                                                                            |
| Agent                  | in       | MCP           | Extraktion/Retrieval                                                                                                                                       | Minimal-Scopes (C-3), Consent (T-6)                                                                 |

---

## 4. Lösungsstrategie (arc42 §4)

| Qualitätsziel                           | Lösungsansatz                                                                                                  | Muster                        |
| --------------------------------------- | -------------------------------------------------------------------------------------------------------------- | ----------------------------- |
| Wartbarkeit, klare Verantwortlichkeiten | **Modularer Monolith** mit **Hexagonaler Architektur** (Ports & Adapters); Fachmodule mit expliziten Verträgen | Ports & Adapters, DDD-Schnitt |
| Datensouveränität (C-1)                 | Alle KI-/Datenpfade in lokalen Containern; Egress-Policy + CI-Test                                             | Deployment-Isolation          |
| ACL-Isolation (NfA-4)                   | Berechtigungs-**Pre-Filter** als Query-Prädikat, nicht Post-Filter                                             | Security-in-depth             |
| Skalierung (Cluster)                    | **Zustandsloser** Applikationskern; Zustand ausschliesslich in Postgres/pgvector                               | Shared-Data, Stateless-App    |
| Agent-Konsum ohne UI-Bruch              | Fachlogik hinter Inbound-Ports; **REST-Adapter** und **MCP-Adapter** teilen denselben Application-Service      | Adapter-Symmetrie             |

**Grundentscheid (siehe ADR-001):** kein Microservice-Split — keine nichtfunktionale Anforderung (Last, unabhängige Deploybarkeit, Team-Topologie) rechtfertigt die Verteilungskosten. Cluster-Betrieb wird durch **Zustandslosigkeit + gemeinsame Datenhaltung** erreicht, nicht durch Service-Zerlegung.

---

## 5. Bausteinsicht — C4 Level 2 (arc42 §5 / Perspektive: Struktur)

### 5.1 Container-Diagramm (L2)

```mermaid
C4Container
    title C4 L2 — Container DocExtract (modularer Monolith)
    Person(sb, "Sachbearbeiter:in")
    System_Ext(agent, "Agent", "via MCP")
    System_Ext(dms, "d.velop DMS-API")
    System_Ext(idp, "d.velop IdP")
    System_Ext(tpa, "Wertelisten-Webhook")

    System_Boundary(tb, "Vertrauensgrenze — lokale Betriebsgrenze (kein Egress)") {
        Container(ng, "Angular Frontend", "Angular/SSR-CSR", "Upload, PDF-Vorschau, Validierungs-UI (iframe)")
        Container(app, "DocExtract Backend", "Java 21 / Spring Boot 4", "Hexagonaler Kern: Ingest, Retrieval, Extraction, Validation, Audit; REST- + MCP-Inbound; zustandslos (cluster-fähig)")
        Container(prev, "Preview-Service", "Gotenberg/LibreOffice", "Dokument → PDF für visuelle Kontrolle")
        Container(doc, "docling", "Container", "Dokument → strukturiertes Markdown")
        Container(llm, "Ollama", "Qwen 3, lokal", "Embeddings + schema-constrained Extraktion")
        ContainerDb(pg, "PostgreSQL + pgvector", "RDBMS", "Metadaten, Vektoren, Audit-Log, Workflow-State")
    }

    Rel(sb, ng, "bedient")
    Rel(ng, app, "REST (Session-Cookie)")
    Rel(agent, app, "MCP")
    Rel(app, prev, "Konvertierung")
    Rel(app, doc, "Strukturierung")
    Rel(app, llm, "Embeddings / Extraktion (nur lesend auf Vektoren)")
    Rel(app, pg, "R/W (JPA/pgvector)")
    Rel(app, idp, "Session-Validierung → Tenant/ACL")
    Rel(app, dms, "Metadaten lesen / Rückschreiben nach Consent")
    Rel(app, tpa, "Wertelisten (JIT)")

    UpdateElementStyle(tb, $borderColor="red")
```

### 5.2 Fachmodule (Bausteine der Ebene 3)

| Modul                      | Verantwortung                                                                                                 | Inbound-Port          | Wichtigste Outbound-Ports                                                                      |
| -------------------------- | ------------------------------------------------------------------------------------------------------------- | --------------------- | ---------------------------------------------------------------------------------------------- |
| **ingest**                 | Upload, Eingangsvalidierung, Limits, Preview, initialer DMS-Chunk-Upload inkl. `Location` (FR-1)              | `IngestDocument`      | `PreviewPort`, `DmsChunkUploadPort` (liefert `Location`)                                       |
| **structuring**            | docling-Aufbereitung → Markdown (FR-2)                                                                        | `StructureDocument`   | `StructuringPort`                                                                              |
| **retrieval**              | Embedding + Ähnlichkeitssuche mit **ACL-Pre-Filter** (FR-3, NfA-4/-6)                                         | `FindSimilar`         | `EmbeddingPort`, `VectorSearchPort`, `AuthContextPort`                                         |
| **extraction**             | Schema-constrained LLM-Attributvorschläge (FR-4, T-1)                                                         | `ExtractAttributes`   | `LlmPort`, `ValueListPort`                                                                     |
| **validation**             | Human-in-the-Loop, finales Attribut-Rückschreiben an die zuvor erhaltene `Location` nach Freigabe (FR-5, C-2) | `ConfirmAndWriteBack` | `DmsWritePort` (separater, authentifizierter Adapter, C-3; adressiert `Location` aus `ingest`) |
| **agentgateway**           | MCP-Tool, teilt Application-Services (FR-6, T-6)                                                              | `McpTool`             | dieselben wie UI-Pfad                                                                          |
| **audit** (Querschnitt)    | Append-only Protokoll, Token-/Kosten-Erfassung (C-4, NfA-7)                                                   | —                     | `AuditLogPort`                                                                                 |
| **security** (Querschnitt) | Session→Tenant/ACL-Auflösung, Consent-Gate                                                                    | `AuthContextPort`     | —                                                                                              |

### 5.3 Paketstruktur (Perspektive: Struktur, textuell)

```

com.adeon.docextract
├─ ingest
│ ├─ domain # DocumentUpload, Limits, ValidationResult, DmsLocation
│ ├─ application # IngestDocumentService (Inbound-Port-Impl, ruft DmsChunkUploadPort)
│ └─ adapter
│ ├─ in.rest # UploadController (REST)
│ ├─ out.preview # GotenbergPreviewAdapter
│ └─ out.dms # DmsChunkUploadAdapter (POST Chunk → liest `Location`-Header)
├─ structuring
│ ├─ application
│ └─ adapter.out.docling
├─ retrieval
│ ├─ domain # SimilarityQuery, AclPredicate, RetrievalResult
│ ├─ application # FindSimilarService (ACL-Pre-Filter!)
│ └─ adapter.out # PgVectorSearchAdapter, OllamaEmbeddingAdapter
├─ extraction
│ ├─ domain # AttributeSchema, ExtractionResult, Confidence
│ ├─ application # ExtractAttributesService (schema-constrained)
│ └─ adapter.out # OllamaLlmAdapter, ValueListWebhookAdapter
├─ validation
│ ├─ domain # ConfirmedAttributes, Provenance
│ ├─ application # ConfirmAndWriteBackService (Consent-Gate C-2, adressiert gespeicherte `Location`)
│ └─ adapter.out.dms # DmsWriteAdapter (POST Attribute an `Location`, separater Dienst, C-3)
├─ agentgateway
│ └─ adapter.in.mcp # McpToolServer (teilt application-Services)
├─ audit # append-only, ohne roh-PII (C-4)
├─ security # AuthContext, TenantAclResolver, ConsentGuard
└─ shared # Fehlerbehandlung, Config, Observability

```

**Verantwortlichkeitsprinzip:** Domäne kennt keine Frameworks; Adapter kennen keine Fachregeln; die `security`- und `audit`-Querschnitte werden über Spring-DI und Aspekte eingezogen, sodass jeder Inbound-Pfad (REST **und** MCP) dieselben Guardrails durchläuft.

---

## 6. Laufzeitsicht (arc42 §6 / Perspektive: Verhalten & Interaktion)

### 6.1 Happy-Path — Upload bis Vorschlag (UI)

```mermaid
sequenceDiagram
    autonumber
    actor SB as Sachbearbeiter:in
    participant FE as Angular (iframe)
    participant API as Backend / Inbound-REST
    participant SEC as security (Tenant/ACL)
    participant ING as ingest
    participant STR as structuring (docling)
    participant RET as retrieval (pgvector)
    participant EXT as extraction (Ollama)
    participant DMS as d.velop DMS-API
    participant AUD as audit

    SB->>FE: Dokument hochladen
    FE->>API: POST /documents (Session-Cookie)
    API->>SEC: Session validieren → Tenant+ACL
    SEC-->>API: AuthContext
    API->>ING: validieren (Limits, Preview) [T-4]
    ING-->>API: ok + PDF-Vorschau
    API->>DMS: POST Dokument-Chunk hochladen
    DMS-->>API: 201 + Header `Location` (Ziel für spätere Finalisierung)
    API->>API: Location am Dokument vermerken (dms_location)
    API->>STR: Dokument → Markdown
    STR-->>API: strukturiertes MD
    API->>RET: FindSimilar(MD, ACL-Pre-Filter) [NfA-4]
    RET-->>API: Top-k Vorlagen (Precision@3)
    API->>EXT: ExtractAttributes(MD, Vorlagen, Werteliste) [schema-constrained, T-1]
    EXT-->>API: JSON-Vorschlag + Konfidenz
    API->>AUD: LLM-Call protokollieren (Token, Hash, ohne PII) [C-4/NfA-7]
    API-->>FE: Vorschläge + Quellen + Konfidenz
    FE-->>SB: Validierungs-UI
```

### 6.2 Freigabe & Rückschreiben (Human-in-the-Loop, C-2)

```mermaid
sequenceDiagram
    autonumber
    actor SB as Sachbearbeiter:in
    participant FE as Angular
    participant API as Backend
    participant VAL as validation
    participant DMS as DmsWriteAdapter (C-3)
    participant AUD as audit

    SB->>FE: korrigiert / bestätigt Attribute
    FE->>API: POST /documents/{id}/confirm
    API->>VAL: ConfirmAndWriteBack(bestätigte Attribute)
    Note over VAL: Consent-Gate — kein Write ohne explizite Freigabe
    VAL->>VAL: gespeicherte `Location` (aus Ingest-Phase) nachschlagen
    VAL->>DMS: POST Attribute an `Location` (finalisiert Dokument, authentifiziert)
    DMS-->>VAL: ok
    VAL->>VAL: optional Vorlage mit Herkunftsmarkierung [T-2]
    VAL->>AUD: Schreib-Event append-only
    API-->>FE: bestätigt
```

### 6.3 Agent-Pfad (MCP) — gleiche Guardrails

Der Agent ruft dasselbe `ExtractAttributes`/`FindSimilar` über den **MCP-Adapter**. Kritische Aktionen (DMS-Write) sind auch hier **consent-pflichtig** (T-6): Das MCP-Tool liefert Vorschläge, ein Schreibvorgang erfordert einen expliziten Freigabeschritt und minimale Scopes (C-3). Fehlerinjektion (ungültiges LLM-JSON, docling-Absturz) endet in einem **protokollierten, sauberen Fehlerzustand** (NfA-3).

---

## 7. Verteilungssicht (arc42 §7 / Deployment)

```mermaid
flowchart TB
    subgraph host["Host — docker compose (Linux / Windows-WSL2)"]
      direction TB
      subgraph tb["Vertrauensgrenze — Egress unterbunden (C-1), CI-Egress-Test"]
        fe["angular-frontend"]
        be1["docextract-backend #1"]
        be2["docextract-backend #2  (Cluster)"]
        prev["preview (Gotenberg/LibreOffice)"]
        doc["docling"]
        llm["ollama (Qwen3, Digest-gepinnt C-5)"]
        db[("postgres + pgvector\nMetadaten · Vektoren · Audit · Workflow-State")]
      end
    end
    proxy["d.velop Reverse Proxy"] --> fe
    fe --> be1
    fe --> be2
    be1 --> prev & doc & llm & db
    be2 --> prev & doc & llm & db
    be1 -. "nur Metadaten" .-> dms["d.velop DMS-API"]
    be1 -. "Session" .-> idp["d.velop IdP"]

    classDef tb fill:#fff3f3,stroke:#c0392b,stroke-width:2px;
    class tb tb;
```

- **Cluster-fähig:** N Backend-Instanzen hinter dem Proxy, gemeinsame DB; keine Sticky Sessions nötig (zustandslos).
- **Reproduzierbar (C-6):** `docker compose up`, Basis-Images per **Digest** fixiert, Modelle per Digest-Pinning (T-5/C-5).
- **CI-Gate:** Smoke- + **Egress-Test** (LLM als Mock), Security-Scan (SAST/Dependency/Image) vor Publish.

---

## 8. Querschnittliche Konzepte (arc42 §8)

### 8.1 Framework-Konzepte (Spring Boot 4)

- **Dependency Injection** trennt Ports von Adaptern; Querschnitte (`security`, `audit`) via DI/Aspekte an jedem Inbound-Pfad.
- **REST**: Ressourcenorientierte Controller, OpenAPI-spezifiziert (Block 3), Fehlerfälle als `ProblemDetail` (RFC 9457).
- **Konfiguration**: `application.yml` + Umgebungsprofile (`local`, `ci`, `cluster`); Secrets nie im Image.
- **Fehlerbehandlung**: zentraler `@ControllerAdvice`; jede Pipeline-Stufe mit eigenem Timeout und definiertem Abbruch (T-4 → NfA-3).

### 8.2 Sicherheit (Überblick, Details § 10.3)

Session-basierte AuthN (d.velop-Cookie) → **Tenant/ACL-Auflösung** → Pre-Filter im Retrieval → Consent-Gate vor irreversiblen Aktionen → append-only Audit ohne roh-PII. Least Privilege: LLM **liest** nur Vektoren; Schreiben über separaten Dienst.

### 8.3 Observability

Strukturiertes Logging pro Pipeline-Stufe (Latenz je Stufe → NfA-2), Metriken (Micrometer/Prometheus), Traces (OpenTelemetry). **KI-Betriebsdaten**: Modell-/Prompt-Version, Token/Kosten, Eval-Resultat, Guardrail-Events.

### 8.4 Datenmodell (arc42 / Krit. 6 — Perspektive: Struktur)

```mermaid
erDiagram
    DOCUMENT ||--o{ EXTRACTION_RUN : "erzeugt"
    EXTRACTION_RUN ||--|| ATTRIBUTE_SUGGESTION : "liefert"
    ATTRIBUTE_SUGGESTION ||--o| CONFIRMATION : "wird bestätigt"
    DOCUMENT ||--o{ DOCUMENT_CHUNK : "wird embedded"
    DOCUMENT_CHUNK ||--|| EMBEDDING : "vektorisiert"
    EXTRACTION_RUN ||--o{ AUDIT_ENTRY : "protokolliert"
    CONFIRMATION ||--o| TEMPLATE : "kann Vorlage werden"

    DOCUMENT {
        uuid id PK
        string tenant_id "NfA-4: Mandant"
        string acl_ref "NfA-4: Berechtigungsprädikat"
        string source_type "E-1..E-5"
        string status "workflow-state (M2)"
        string dms_location "Location-Header aus initialem Chunk-Upload, Ziel der Finalisierung"
        timestamptz retention_until "NfA-5: Löschfrist"
    }
    DOCUMENT_CHUNK {
        uuid id PK
        uuid document_id FK
        text markdown "docling-Output (FR-2)"
    }
    EMBEDDING {
        uuid chunk_id FK
        vector embedding "pgvector, ACL-pre-filterbar"
        string tenant_id
        string acl_ref
    }
    EXTRACTION_RUN {
        uuid id PK
        uuid document_id FK
        string model_version "C-5 Digest"
        int token_count "NfA-7"
        numeric confidence
    }
    ATTRIBUTE_SUGGESTION {
        uuid id PK
        uuid run_id FK
        jsonb payload "schema-validiert (T-1)"
    }
    CONFIRMATION {
        uuid id PK
        uuid suggestion_id FK
        string confirmed_by
        timestamptz confirmed_at "C-2 Consent"
    }
    TEMPLATE {
        uuid id PK
        string provenance "Herkunftsmarkierung (T-2)"
        boolean quarantined
    }
    AUDIT_ENTRY {
        uuid id PK
        uuid run_id FK
        string prompt_hash "ohne roh-PII (C-4)"
        int token_count
        string event_type
        timestamptz created_at "append-only"
    }
```

**Migrationsstrategie:** versionierte SQL-Migrationen (Flyway/Liquibase); pgvector-Index (HNSW/IVFFlat) mit `tenant_id`/`acl_ref` als Filterspalten, damit der **Pre-Filter** Teil der Query, nicht ein Post-Filter ist. Löschpfad (NfA-5) kaskadiert Dokument + Chunks + Embeddings; Audit bleibt referenzierend (Hashes) erhalten.

### 8.5 KI-Integration (Krit. 16 — substanzielle, abgesicherte KI-Funktion)

- **Retrieval (FR-3):** lokales Embedding, pgvector-Suche **nach** ACL-Pre-Filter → Precision@3 (NfA-6).
- **Extraktion (FR-4):** schema-constrained Decoding gegen JSON-Schema; Werteliste als erlaubte Domäne; „unbekannt" statt Halluzination (E-5).
- **Absicherung:** Dokument strikt als _untrusted data_ (T-1), keine ableitbaren Tool-Calls (C-2), Least Privilege (C-3), append-only Audit (C-4).

---

## 9. Architekturentscheidungen — ADRs (arc42 §9 / Krit. 18: bewusst nicht delegiert)

Diese drei Entscheidungen wurden **nicht an die KI delegiert**; sie tragen die Modulbotschaft (Entwerfen/Prüfen statt Implementieren).

### ADR-001 — Modularer Monolith statt Microservices

- **Status:** akzeptiert · **Kontext:** Trennung von Frontend/Services/Persistenz gefordert, aber keine NfA zu unabhängiger Skalierung/Deployment.
- **Entscheidung:** Ein deploybarer, **zustandsloser** Monolith mit hexagonalen Fachmodulen; Cluster über gemeinsame Datenhaltung.
- **Begründung (gemessen an Qualität):** Wartbarkeit & Latenz (kein Netz-Hop zwischen Modulen, NfA-2) schlagen Verteilungsflexibilität; Microservices würden Betriebs-/Konsistenzkosten ohne NfA-Nutzen erzeugen.
- **Konsequenz:** Modulgrenzen müssen im Code hart bleiben (Paketstruktur, Verträge), damit ein späterer Split möglich bliebe.

### ADR-002 — Berechtigungs-Pre-Filter im Retrieval (nicht Post-Filter)

- **Status:** akzeptiert · **Kontext:** Cross-Tenant-/Cross-ACL-Leak (T-3, NfA-4). Reine Vektorähnlichkeit kennt keine Berechtigung.
- **Entscheidung:** Tenant/ACL-Prädikate aus der Session werden **als Filter in die Vektor-Query** eingebettet.
- **Begründung:** Post-Filter kann Treffer bereits „gesehen" haben (Timing/Ranking-Leak); Pre-Filter garantiert **0 Fremdtreffer** und ist testbar (Cross-Tenant-Test).
- **Konsequenz:** Embeddings tragen `tenant_id`/`acl_ref`; Index muss filterfähig sein.

### ADR-003 — Human-in-the-Loop mit hartem Consent-Gate (auch für MCP)

- **Status:** akzeptiert · **Kontext:** Irreversible DMS-Writes; Agent kann Aufrufe verketten (T-6).
- **Entscheidung:** **Keine** LLM-Ausgabe löst selbsttätig einen Schreib-/Tool-Call aus; Write nur nach expliziter menschlicher Freigabe — UI **und** MCP identisch (C-2).
- **Begründung:** Datenschutz/Integrität vor Komfort; ein einziger, gemeinsamer Application-Service erzwingt das Gate für beide Inbound-Pfade.
- **Konsequenz:** Adapter-Symmetrie; `DmsWriteAdapter` ist der einzige Schreibpfad, separat authentifiziert (C-3). Der initiale Chunk-Upload beim Ingest (liefert `Location`) legt lediglich einen unbestätigten Rohdatensatz ohne Attribute an und gilt **nicht** als irreversibler Write; erst die attribut-tragende Finalisierung an dieser `Location` erfordert das Consent-Gate.

> Weitere ADRs pro Block: Präsentationsschicht (Block 2), Kommunikations-/Async-Entscheid (Block 3), Vektor-DB-Integration & Persistenzmuster (Block 4).

---

## 10. Test-, Sicherheits- und Betriebsstrategie (arc42 §11 / Raster Validierung)

### 10.1 Teststrategie (Krit. 12/13)

| Stufe            | Umfang                                                               | Bezug          |
| ---------------- | -------------------------------------------------------------------- | -------------- |
| Unit             | Domänenlogik (Limits, Schema-Validierung, ACL-Prädikat)              | FR-1/-3/-4     |
| Integration      | Adapter (pgvector, docling, Ollama-Mock)                             | § 5.2          |
| Fehlerinjektion  | ungültiges LLM-JSON, docling-Absturz, OCR-Müll → sauberer Endzustand | NfA-3          |
| Cross-Tenant/ACL | automatisierter Zugriffstest, 0 Fremdtreffer                         | NfA-4          |
| Eval (KI)        | Soll/Ist-JSON gegen Eval-Set (M2 provisorisch, M3 belastbar)         | NfA-1/-2/-6/-7 |

### 10.2 Abnahmekriterien (Krit. 11)

Pro Kernfunktion FR-1…FR-6 ein prüfbares Exit-Kriterium (siehe SPEC § 7 Meilensteine M1–M3); KI-Funktion zusätzlich mit Eval-Qualität, Latenz p95, Kostenindikator und Guardrail-Verhalten.

### 10.3 Bedrohungsmodell (Zusammenfassung SPEC § 6)

T-1 Prompt Injection → schema-constrained + untrusted data · T-2 Datenvergiftung → Freigabe + Herkunft/Quarantäne · T-3 ACL-Leak → Pre-Filter · T-4 DoS → Limits/Timeouts · T-5 Modellmanipulation → Digest-Pinning · T-6 MCP-Missbrauch → Consent + minimale Scopes.

### 10.4 DevSecOps-Gate

Mind. ein automatischer Security-Check (SAST/Dependency/Secret/Image/IaC) mit interpretiertem Befund; Publish erst nach bestandenem Gate.

---

## 11. Risiken & technische Schulden (arc42 §11)

- **iGPU-Inferenzlatenz** kann NfA-2 (p95) für E-4 gefährden → Modellwahl/Chunking als Stellhebel, in M2 messen.
- **Self-Learning-Loop** (Ausbaustufe) birgt Datenvergiftungsrisiko → bewusst hinter Freigabe/Quarantäne, in Block-Iterationen verfeinern.
- **Modulgrenzen im Monolith** können erodieren → ArchUnit-Tests zur Durchsetzung der Paketabhängigkeiten empfohlen.

---

## 12. Glossar (arc42 §12)

**ACL-Pre-Filter** – Berechtigungsprädikat als Teil der Vektor-Query · **Guardrail** – unverhandelbare Leitplanke für den KI-Anteil · **Vertrauensgrenze** – lokale Betriebsgrenze ohne Egress (C-1) · **HITL** – Human-in-the-Loop · **MCP** – Model Context Protocol (Agent-Schnittstelle). Weitere Begriffe siehe [SPEC.md](SPEC.md).
