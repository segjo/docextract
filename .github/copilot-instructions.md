# Copilot Instructions — DocExtract

> KI-gestützte Dokumenterfassung & -verschlagwortung für **d.velop documents**.
> Diese Datei steuert GitHub Copilot / Copilot Chat. **Bindend** sind die Guardrails (§ Guardrails) und die drei ADRs.
> Bezugsdokumente: [Architektur (arc42/C4)](../doc/ARCHITECTURE.md), [Spezifikation](../doc/SPEC.md),
> [Dashboard App API](../doc/dashboard-app-api.yaml), [DMS App API](../doc/dvelop-dmsapp.yaml) und
> [Identity Provider API](../doc/identityprovider-api.yaml).

---

## 1. Was DocExtract ist

DocExtract erfasst eingehende Dokumente **ohne manuelles Abtippen**, schlägt Metadaten per KI vor und schreibt sie **erst nach menschlicher Freigabe** ins DMS zurück. Zwei gleichwertige Konsumenten:

- **Sachbearbeiter:in** → über das Angular-Frontend (als **iframe** hinter dem d.velop Reverse Proxy).
- **Agent** → über ein **MCP-Tool**.

Beide Pfade teilen **denselben Application-Service** und **dieselben Guardrails**.

---

## 2. Technologie-Stack (Randbedingungen — nicht eigenmächtig ändern)

| Bereich         | Vorgabe                                                        |
| --------------- | -------------------------------------------------------------- |
| Sprache/Runtime | **Java 21**                                                    |
| Backend         | **Spring Boot 4**                                              |
| Frontend        | **Angular** (iframe, Upload / PDF-Vorschau / Validierungs-UI)  |
| Persistenz      | **PostgreSQL + pgvector**                                      |
| Strukturierung  | **docling** (Dokument → Markdown)                              |
| LLM/Embeddings  | **Ollama** (Qwen 3, lokal, Digest-gepinnt)                     |
| Preview         | **Gotenberg / LibreOffice**                                    |
| Betrieb         | **Docker Compose** (Linux **und** Windows/WSL2), Cluster-Modus |
| AuthN           | **d.velop-Cookie/Session** (keine Impersonation)               |
| Agent-API       | **MCP (Model Context Protocol)**                               |
| REST-Fehler     | **ProblemDetail (RFC 9457)** via zentralem `@ControllerAdvice` |

> Neue Abhängigkeiten oder Frameworks nur mit ausdrücklicher Begründung vorschlagen; **kein** externer KI-/Cloud-Dienst (siehe Vertrauensgrenze).

---

## 3. Architektur-Prinzipien (immer einhalten)

- **Modularer Monolith** — ein deploybares Artefakt, **kein** Microservice-Split (ADR-001).
- **Hexagonale Architektur** (Ports & Adapters) mit DDD-Schnitt.
- **Zustandsloser Kern** — sämtlicher Zustand ausschliesslich in Postgres/pgvector (Cluster-fähig, keine Sticky Sessions).
- **Adapter-Symmetrie** — REST-Adapter und MCP-Adapter rufen **identische** Application-Services.
- **Querschnitte** (`security`, `audit`) werden per Spring-DI/Aspekte an **jedem** Inbound-Pfad eingezogen.

### Abhängigkeitsregeln (hart)

- `domain` kennt **keine** Frameworks (kein Spring, kein JPA, kein Jackson-Zwang).
- `adapter` kennt **keine** Fachregeln.
- Fluss: `adapter.in → application (Port) → domain`; Ausgänge nur über `application → adapter.out (Port)`.
- Modulgrenzen mit **ArchUnit**-Tests durchsetzen (verhindert Erosion → technische Schuld).

---

## 4. Modul- & Paketstruktur

Basis-Package: `ch.adeon.apps.docextract`

| Modul          | Verantwortung                                                                                | Inbound-Port          |
| -------------- | -------------------------------------------------------------------------------------------- | --------------------- |
| `ingest`       | Upload, Eingangsvalidierung/Limits, Preview, initialer DMS-Chunk-Upload (liefert `Location`) | `IngestDocument`      |
| `structuring`  | docling → strukturiertes Markdown                                                            | `StructureDocument`   |
| `retrieval`    | Embedding + Ähnlichkeitssuche mit **ACL-Pre-Filter**                                         | `FindSimilar`         |
| `extraction`   | Schema-constrained LLM-Attributvorschläge                                                    | `ExtractAttributes`   |
| `validation`   | Human-in-the-Loop, finales Rückschreiben an gespeicherte `Location`                          | `ConfirmAndWriteBack` |
| `agentgateway` | MCP-Tool, teilt die Application-Services des UI-Pfads                                        | `McpTool`             |
| `audit`        | Querschnitt: append-only Protokoll, Token-/Kostenerfassung (**ohne roh-PII**)                | —                     |
| `security`     | Querschnitt: Session→Tenant/ACL-Auflösung, Consent-Gate                                      | `AuthContextPort`     |

Paketkonvention pro Modul:

```
<modul>/
├─ domain        # reine Fachobjekte, keine Frameworks
├─ application   # Inbound-Port-Implementierungen, orchestriert Outbound-Ports
└─ adapter/
   ├─ in.rest    # Controller
   ├─ in.mcp     # MCP-Server (nur agentgateway)
   └─ out.*      # DB, docling, Ollama, Preview, DMS, Webhook
```

---

## 5. Guardrails — UNVERHANDELBAR

Diese Regeln haben Vorrang vor jeder Bequemlichkeit. Code, der sie verletzt, ist falsch.

1. **Vertrauensgrenze / kein Egress (C-1):** Dokumentinhalte, Markdown und Embeddings verlassen die lokale Betriebsgrenze **nie**. Erlaubte Ausgänge ausschliesslich: d.velop **IdP**, d.velop **DMS-API** (nur Metadaten), **Wertelisten-Webhook**. Keine anderen ausgehenden Netzaufrufe generieren.
2. **ACL-Pre-Filter (ADR-002, T-3/NfA-4):** Tenant/ACL-Prädikate aus der Session gehören **in die Vektor-Query** (Filterspalten `tenant_id`, `acl_ref`). **Niemals** Post-Filtering auf bereits gerankte Treffer.
3. **Consent-Gate / Human-in-the-Loop (ADR-003, C-2):** **Keine** LLM-Ausgabe löst selbsttätig einen DMS-Write oder Tool-Call aus. Schreiben nur nach expliziter menschlicher Freigabe — **UI und MCP identisch**.
4. **Least Privilege (C-3):** Das LLM **liest** nur Vektoren. Schreiben ausschliesslich über den separat authentifizierten `DmsWriteAdapter` (einziger Schreibpfad).
5. **Untrusted Data (T-1):** Dokumentinhalt ist grundsätzlich **untrusted**. Extraktion strikt **schema-constrained** gegen JSON-Schema; bei Unsicherheit `"unbekannt"` statt Halluzination (E-5). Keine aus dem Dokument ableitbaren Tool-Calls.
6. **Audit append-only, ohne roh-PII (C-4):** Nur Hashes/Metadaten (`prompt_hash`, `token_count`, `event_type`) protokollieren — niemals Roh-PII.
7. **Digest-Pinning (C-5/T-5):** Basis-Images und Modelle per **Digest** fixieren, nicht per mutable Tag.

---

## 6. Kern-Workflow (Zwei-Phasen-DMS)

1. **Ingest:** Validierung + Preview → **Chunk-Upload** zur DMS-API → `Location`-Header am Dokument als `dms_location` merken. _(Roh-Datensatz ohne Attribute — gilt NICHT als irreversibler Write.)_
2. **Structuring:** docling → Markdown.
3. **Retrieval:** `FindSimilar` mit **ACL-Pre-Filter** → Top-k Vorlagen.
4. **Extraction:** schema-constrained Vorschlag + Konfidenz; LLM-Call auditieren.
5. **Validation:** Sachbearbeiter:in bestätigt → **Consent-Gate** → Finalisierung: Attribute an gespeicherte `Location` schreiben (`DmsWriteAdapter`) → append-only Audit-Event.

---

## 7. Spring-Boot-Idiome

- **DI** trennt Ports von Adaptern; Konstruktor-Injection bevorzugen.
- **REST:** ressourcenorientiert, OpenAPI-spezifiziert; Fehler als `ProblemDetail` (RFC 9457) über zentralen `@ControllerAdvice`.
- **Config:** `application.yml` + Profile `local`, `ci`, `cluster`; **Secrets nie im Image**.
- **Fehlerbehandlung:** jede Pipeline-Stufe mit eigenem **Timeout** und definiertem sauberem Abbruch (T-4 → NfA-3).
- **Observability:** strukturiertes Logging pro Stufe (Latenz je Stufe), Micrometer/Prometheus-Metriken, OpenTelemetry-Traces; KI-Betriebsdaten (Modell-/Prompt-Version, Token/Kosten, Guardrail-Events).

---

## 8. Datenmodell (Auszug)

Zentrale Entitäten: `DOCUMENT` (u. a. `tenant_id`, `acl_ref`, `status`, `dms_location`, `retention_until`), `DOCUMENT_CHUNK`, `EMBEDDING` (mit `tenant_id`/`acl_ref` als **Filterspalten**), `EXTRACTION_RUN`, `ATTRIBUTE_SUGGESTION` (`jsonb`, schema-validiert), `CONFIRMATION` (Consent-Zeitpunkt), `TEMPLATE` (Herkunft/Quarantäne), `AUDIT_ENTRY` (append-only, `prompt_hash` ohne PII).

- **Migrationen:** versioniert (**Flyway/Liquibase**).
- **pgvector-Index:** HNSW/IVFFlat **mit** `tenant_id`/`acl_ref` als Filterspalten (Pre-Filter Teil der Query).
- **Löschpfad (NfA-5):** kaskadiert Dokument + Chunks + Embeddings; Audit bleibt referenzierend (Hashes) erhalten.

---

## 9. Teststrategie (bei jedem Feature mitdenken)

| Stufe            | Fokus                                                                |
| ---------------- | -------------------------------------------------------------------- |
| Unit             | Domänenlogik (Limits, Schema-Validierung, ACL-Prädikat)              |
| Integration      | Adapter (pgvector, docling, **Ollama-Mock**)                         |
| Fehlerinjektion  | ungültiges LLM-JSON, docling-Absturz, OCR-Müll → sauberer Endzustand |
| Cross-Tenant/ACL | automatisierter Zugriffstest, **0 Fremdtreffer**                     |
| Eval (KI)        | Soll/Ist-JSON gegen Eval-Set                                         |
| Architektur      | **ArchUnit** — Paketabhängigkeiten/Modulgrenzen                      |

**CI-Gate:** Smoke- + **Egress-Test** (LLM als Mock) + Security-Scan (SAST/Dependency/Secret/Image/IaC). Publish erst nach bestandenem Gate.

---

## 10. Bedrohungsmodell → Mitigation (Kurzreferenz)

`T-1` Prompt Injection → schema-constrained + untrusted data · `T-2` Datenvergiftung → Freigabe + Herkunft/Quarantäne · `T-3` ACL-Leak → **Pre-Filter** · `T-4` DoS → Limits/Timeouts · `T-5` Modellmanipulation → Digest-Pinning · `T-6` MCP-Missbrauch → Consent + minimale Scopes.

---

## 11. Do / Don't für generierten Code

**Do**

- Fachlogik hinter Inbound-Ports; REST **und** MCP über denselben Service.
- Tenant/ACL immer aus dem `AuthContext` ableiten, nie aus Client-Parametern vertrauen.
- Timeouts, Fehler-`ProblemDetail` und Audit-Eintrag pro Pipeline-Stufe.

**Don't**

- Keinen Microservice/Service-Split ohne begründete NfA vorschlagen.
- Keine Post-Filter-Berechtigungsprüfung im Retrieval.
- Keinen automatischen DMS-Write ohne Consent-Gate.
- Keine externen KI-/Cloud-Aufrufe, kein Roh-PII in Logs, keine mutable Image-Tags.

---

## 12. Commit-Nachrichten

- Verwende fuer alle Commit-Nachrichten **Conventional Commits** im Format
  `<type>(<scope>): <beschreibung>`.
- Erlaubte Typen sind mindestens `feat`, `fix`, `docs`, `test`, `refactor`,
  `build`, `ci`, `chore` und `perf`.
- Der Scope ist optional und beschreibt das betroffene Modul, z. B.
  `feat(retrieval): add ACL-filtered similarity search`.
- Kennzeichne inkompatible Aenderungen mit `!` nach Typ oder Scope und erlaeutere
  sie zusaetzlich mit einem `BREAKING CHANGE:`-Footer.
