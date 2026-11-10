# DocExtract

KI-gestützte Dokumenterfassung und Verschlagwortung für **d.velop documents**.

DocExtract verarbeitet eingehende Dokumente lokal, schlägt passende Metadaten vor und zeigt ähnliche, bereits freigegebene Dokumente als fachliche Referenz an. Erst nach einer expliziten menschlichen Freigabe werden bestätigte Attribute ins DMS zurückgeschrieben.

> **Projektkontext:** CAS AISE
> **Status:** In Entwicklung

## Ziel

DocExtract reduziert den manuellen Aufwand bei der Dokumenterfassung:

1. Dokument hochladen
2. Vorschau bereitstellen
3. Dokument mit docling strukturieren und in kontextualisierte Chunks zerlegen
4. Ähnliche freigegebene Dokumente über Embeddings und pgvector finden
5. Metadaten mit einem lokalen LLM vorschlagen
6. Vorschläge durch einen Menschen prüfen und korrigieren
7. Bestätigte Attribute ins DMS zurückschreiben

Das System folgt den Prinzipien **Human-in-the-Loop**, **Datensouveränität**, **Least Privilege**, **Datenminimierung** und **Nachvollziehbarkeit**.

## Kernfunktionen

- Upload einzelner Dokumente über Frontend oder REST
- PDF-Vorschau im Browser
  - native PDFs ohne Konvertierung
  - Nicht-PDFs über Gotenberg/LibreOffice
- Strukturierung und natives Chunking mit docling
- Lokale Embedding-Erzeugung und Ähnlichkeitssuche mit Berechtigungs-Pre-Filter
- Schema-validierte Attributvorschläge durch ein lokales LLM
- Validierungsoberfläche mit Konfidenz- und Quellenanzeige
- Rückschreiben ins DMS ausschliesslich nach expliziter Freigabe
- MCP-Schnittstelle mit denselben Berechtigungs- und Consent-Regeln wie der UI-Pfad
- Asynchrone Verarbeitung mit Fortschrittsmeldungen über Server-Sent Events
- Cluster-fähiger Betrieb ohne zusätzlichen Message Broker

## Architektur

DocExtract ist als **modularer Monolith mit hexagonaler Architektur** aufgebaut. Die Fachmodule laufen gemeinsam in einem Spring-Boot-Prozess und kommunizieren über definierte Ports. Die Modulgrenzen werden logisch getrennt und mit ArchUnit abgesichert.

### Fachmodule

- `ingest`: Upload, Limits und Vorschau
- `structuring`: docling-Verarbeitung und HybridChunker
- `retrieval`: Embeddings, Quarantäne und Ähnlichkeitssuche
- `extraction`: Schema-validierte Attributextraktion
- `validation`: Freigabe, DMS-Write und Korpus-Promotion
- `agentgateway`: MCP-Adapter
- `process`: Asynchrone Orchestrierung, SSE und Recovery
- `audit`: Append-only Audit ohne Roh-PII
- `security`: Session-, Tenant-, ACL- und Consent-Prüfung

Weitere Details befinden sich in [doc/ARCHITECTURE.md](doc/ARCHITECTURE.md).

## Technologie-Stack

- Java 21
- Spring Boot 4
- Angular
- PostgreSQL mit pgvector
- docling mit HybridChunker
- Ollama mit getrenntem Embedding-Modell und LLM
- Gotenberg/LibreOffice für Vorschauen von Nicht-PDFs
- Docker Compose
- Micrometer, Prometheus und OpenTelemetry

## Daten- und Embedding-Lifecycle

`DoclingDocument` und Chunks enthalten potenziell sensible Inhalte und bleiben deshalb ausschliesslich im Arbeitsspeicher des laufenden Jobs.

Die einmalig erzeugten Dokument-Embeddings werden zunächst als `PENDING` mit TTL in pgvector gespeichert. Sie sind in diesem Zustand nicht retrievalfähig. Nach der menschlichen Freigabe werden dieselben Vektoren ohne Neuberechnung zu `APPROVED` hochgestuft. Bei Ablehnung, endgültigem Prozessfehler oder Ablauf werden sie gelöscht.

Retrieval-Abfragen berücksichtigen ausschliesslich `APPROVED`-Embeddings und erzwingen Tenant- und ACL-Prädikate als Pre-Filter.

## Sicherheitsprinzipien

- Dokumentinhalte, Chunks, Prompts, LLM-Kontexte und Embeddings bleiben innerhalb der lokalen Betriebsgrenze.
- Keine LLM-Ausgabe löst selbstständig einen DMS-Write oder kritischen Tool-Aufruf aus.
- Der Inferenzdienst besitzt keinen direkten Zugriff auf PostgreSQL oder die DMS-API.
- Jeder LLM- und Tool-Aufruf wird ohne Roh-PII nachvollziehbar protokolliert.
- Modelle und Betriebsartefakte werden per Digest gepinnt.
- Eingabedokumente werden mit Grössen-, Seiten- und Zeitlimits verarbeitet.
- UI und MCP verwenden dieselben Application Services und Guardrails.

## Asynchrone Verarbeitung

Nach dem synchronen Upload des Originals antwortet das Backend mit `202 Accepted` und einer `processId`. Das Frontend empfängt den weiteren Fortschritt über Server-Sent Events.

Im Cluster erfolgt der Event-Fan-out über PostgreSQL `LISTEN/NOTIFY`. Eine PII-freie `PROCESS_STEP`-Tabelle dient als persistente Catch-up-Quelle für Reconnects. Ein externer Message Broker ist nicht erforderlich.

## Betrieb

Der Stack ist für einen reproduzierbaren Betrieb mit Docker Compose auf folgenden Plattformen vorgesehen:

- Linux
- Windows mit WSL2

Die konkrete Compose-Konfiguration und die erforderlichen Umgebungsvariablen werden zusammen mit der Implementierung im Repository bereitgestellt. Verbindliche Betriebs- und Sicherheitsentscheidungen sind in den Architekturentscheidungen dokumentiert.

### Audit-Schlüssel (`DOCEXTRACT_AUDIT_ACTOR_KEY`)

Das Audit-Protokoll (`audit_entry`) speichert nie die Benutzer-ID, sondern einen Pseudonym-Hash
(`actor_hash` = HMAC-SHA256 über Mandant und User-ID, [ARCHITECTURE.md § 8.6](doc/ARCHITECTURE.md)).
Der HMAC-Schlüssel kommt aus der Umgebungsvariable `DOCEXTRACT_AUDIT_ACTOR_KEY`
(Property `docextract.audit.actor-key`).

| Regel                 | Wert                                                                                      |
| --------------------- | ----------------------------------------------------------------------------------------- |
| Pflicht               | Ja. Ohne Wert bricht der Start ab (Ausnahme: Profil `dev`, dort Dev-Schlüssel mit `WARN`) |
| Mindestlänge          | 32 Zeichen, in jedem Profil                                                               |
| Nicht wiederverwenden | Muss sich von `DOCEXTRACT_TENANT_APP_SECRET` unterscheiden                                |
| Identisch             | Auf allen Backend-Instanzen eines Clusters derselbe Wert                                  |
| Stabil                | Über Neustarts hinweg gleich; ein Wechsel macht alte und neue `actor_hash` unvergleichbar |
| Geheimhaltung         | Nicht ins Repository, nicht ins Image; nur als Umgebungsvariable oder Secret              |

Schlüssel einmal erzeugen und in einer lokalen `.env` ablegen (`.env` ist in `.gitignore`; Docker
Compose liest sie automatisch):

```bash
echo "DOCEXTRACT_AUDIT_ACTOR_KEY=$(openssl rand -base64 48)" >> .env
```

Im Container wird der Wert nur durchgereicht, zum Beispiel für einen Backend-Service in Compose:

```yaml
environment:
  DOCEXTRACT_AUDIT_ACTOR_KEY: ${DOCEXTRACT_AUDIT_ACTOR_KEY:?in .env setzen}
```

Ein automatisches Erzeugen beim Containerstart (z. B. im Entrypoint) ist bewusst nicht vorgesehen: jede
Instanz und jeder Neustart bekäme einen anderen Schlüssel, die Hashes derselben Person wären nicht mehr
vergleichbar. Im Cloud-Betrieb gehört der Wert in den Secret-Store der Plattform.

## Manuelles End-to-End-Testen

Runnable HTTP-Requests gegen die laufende App (d.velop-Mock + echtes Gotenberg statt Test-Doubles) liegen unter [e2e/](e2e/README.md), inklusive Upload-Beispielen für native PDFs und Nicht-PDFs.

### Lokale Entwicklung (dev mode)

1. d.velop-Mock (DMS/IdP-Simulation) starten:

   ```bash
   docker compose up -d dvelop-mock llm-mock
   ```

2. Anwendung bauen und starten (Profil `dev`, mit Dev-Audit-Schlüssel; ohne Profil `dev` ist
   `DOCEXTRACT_AUDIT_ACTOR_KEY` Pflicht, siehe [Audit-Schlüssel](#audit-schlüssel-docextract_audit_actor_key)):

   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

   Ohne weitere Konfiguration verwendet die App den d.velop- und den LLM-Mock aus Schritt 1
   (`docextract.dvelop.*`, `docextract.ollama.base-uri`, `docextract.adapters.llm.*`,
   `docextract.adapters.embedding.*` in
   [application.yml](src/main/resources/application.yml)). Alle Umgebungsvariablen, die diese
   Defaults überschreiben können:

   | Variable                                 | Property                                 | Default (Mock)                                                                                        |
   | ---------------------------------------- | ---------------------------------------- | ----------------------------------------------------------------------------------------------------- |
   | `DOCEXTRACT_AUDIT_ACTOR_KEY`             | `docextract.audit.actor-key`             | — (Pflicht ausser Profil `dev`, siehe [Audit-Schlüssel](#audit-schlüssel-docextract_audit_actor_key)) |
   | `DOCEXTRACT_TENANT_APP_SECRET`           | `docextract.tenant.app-secret`           | — (leer = Single-Tenant)                                                                              |
   | `DOCEXTRACT_AGENT_GROUP_ID`              | `docextract.agent.group-id`              | — (leer; Pflicht wenn MCP aktiv)                                                                      |
   | `DOCEXTRACT_DVELOP_JSTORE_URI`           | `docextract.dvelop.jstore-uri`           | `http://localhost:6380/store/httpgateway/appconfig`                                                   |
   | `DOCEXTRACT_DVELOP_SYSTEM_BASE_URI`      | `docextract.dvelop.system-base-uri`      | `http://localhost:8081` (dvelop-mock)                                                                 |
   | `DOCEXTRACT_OLLAMA_BASE_URI`             | `docextract.ollama.base-uri`             | `http://localhost:8082` (llm-mock)                                                                    |
   | `DOCEXTRACT_ADAPTERS_LLM_BASE_URL`       | `docextract.adapters.llm.base-url`       | `http://localhost:8082` (llm-mock)                                                                    |
   | `DTRACK_ANALYZER_OPENAPI_TOKEN`          | `docextract.adapters.llm.api-key`        | — (leer)                                                                                              |
   | `DOCEXTRACT_ADAPTERS_LLM_MODEL_ID`       | `docextract.adapters.llm.model-id`       | `gpt-5.6-sol`                                                                                         |
   | `DOCEXTRACT_ADAPTERS_EMBEDDING_BASE_URL` | `docextract.adapters.embedding.base-url` | `${DOCEXTRACT_OLLAMA_BASE_URI}/v1` (llm-mock)                                                         |
   | `DOCEXTRACT_ADAPTERS_EMBEDDING_API_KEY`  | `docextract.adapters.embedding.api-key`  | — (leer)                                                                                              |
   | `DOCEXTRACT_ADAPTERS_EMBEDDING_MODEL_ID` | `docextract.adapters.embedding.model-id` | `qwen3-embedding:0.6b`                                                                                |

   Für einen echten d.velop-/LLM-/Embedding-Endpoint lokal die entsprechenden Variablen in die
   (git-ignorierte) `.env` eintragen, z. B.:

   ```bash
   cat >> .env <<EOF
   DOCEXTRACT_AUDIT_ACTOR_KEY=$(openssl rand -base64 48)
   DOCEXTRACT_DVELOP_JSTORE_URI=http://localhost:6380/store/httpgateway/appconfig
   DOCEXTRACT_DVELOP_SYSTEM_BASE_URI=https://dms.local
   DOCEXTRACT_OLLAMA_BASE_URI=http://localhost:11435
   DOCEXTRACT_ADAPTERS_LLM_BASE_URL=https://adeon-drack-analyzer.openai.azure.com
   DTRACK_ANALYZER_OPENAPI_TOKEN=...
   DOCEXTRACT_ADAPTERS_LLM_MODEL_ID=...
   DOCEXTRACT_ADAPTERS_EMBEDDING_BASE_URL=...
   DOCEXTRACT_ADAPTERS_EMBEDDING_API_KEY=...
   DOCEXTRACT_ADAPTERS_EMBEDDING_MODEL_ID=...
   EOF
   ```

   Das Heredoc ist bewusst unquoted (`<<EOF` statt `<<'EOF'`), damit `$(openssl rand -base64 48)`
   direkt in `.env` geschrieben wird; bereits in `.env` vorhandene Zeilen (z. B. ein zuvor erzeugter
   Audit-Schlüssel) vorher entfernen oder die Zeile aus dem Heredoc streichen, um Duplikate zu
   vermeiden.

   VS Code liest `.env` automatisch über `envFile` in [.vscode/launch.json](.vscode/launch.json);
   bei `mvn spring-boot:run` muss sie zuvor in die Shell exportiert werden:

   ```bash
   set -a; source .env; set +a
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

3. Aufruf testen:

   ```bash
   curl -X GET "http://localhost:8080/adeon-docextract/health"
   ```

4. Swagger UI öffnen:

   ```
   http://localhost:8080/adeon-docextract/swagger-ui/index.html
   ```

   Über den Button **Authorize** kann ein Bearer-Token hinterlegt werden, das Swagger UI danach
   automatisch als `Authorization: Bearer <token>`-Header an alle Backend-Requests anhängt.

5. Eigenen API-Vertrag ([doc/api/openapi.yaml](doc/api/openapi.yaml)) nach einer Änderung an Controllern
   oder DTOs neu generieren (ohne laufende App/Postgres; `OpenApiContractTest` schlägt sonst wegen
   Drift fehl):

   ```bash
   mvn test -Dtest=OpenApiContractTest -Dopenapi.write=true
   ```

### MCP-Tool lokal testen

Lesende Tools `get_process_suggestions` und `find_similar_documents` für Agenten (FR-6,
[ARCHITECTURE.md § 6.3](doc/ARCHITECTURE.md)). Der MCP-Endpunkt ist standardmässig aus.

Der Agent meldet sich als **technischer d.velop-User** an: dessen API-Key geht als
`Authorization: Bearer <API-Key>` mit und wird wie bei REST vom `DvelopAuthenticationFilter` beim
d.velop IdP geprüft. Einen eigenen DocExtract-Token gibt es nicht.

An den Agenten gehen weder `sourceExcerpt` noch die Eigenschaftswerte der ähnlichen Dokumente
(`RetrievalResult.properties`). Von ähnlichen Dokumenten erhält er nur `documentId`, `repositoryId`
und `score`. Die Attributvorschläge (`value`/`values`, `confidence`) des eigenen Dokuments gehen an
ihn und können Personendaten enthalten. Die Eigenschaftswerte wurden mit der Session des Benutzers
geladen; über die `documentId` kann der Agent sie mit seinen eigenen DMS-Rechten abfragen.

1. d.velop-Mock, LLM-Mock und Postgres starten, Anwendung mit MCP starten:

   ```bash
   docker compose --profile mock up -d postgres dvelop-mock llm-mock
   DOCEXTRACT_ADAPTERS_MCP_ENABLED=true DOCEXTRACT_DVELOP_SYSTEM_BASE_URI=http://localhost:8081 \
     mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

2. Session eröffnen (Streamable HTTP). Lokal akzeptiert der d.velop-Mock jedes Token; im echten
   System steht hier der API-Key des technischen Agent-Users:

   ```bash
   MCP=http://localhost:8080/adeon-docextract/api/v1/mcp
   HDR=(-H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' \
        -H 'Authorization: Bearer <API-Key des Agent-Users>')
   SID=$(curl -s -D - -o /dev/null "${HDR[@]}" -X POST $MCP -d '{"jsonrpc":"2.0","id":1,"method":"initialize",
     "params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"curl","version":"1"}}}' \
     | grep -i '^mcp-session-id' | cut -d' ' -f2 | tr -d '\r')
   curl -s "${HDR[@]}" -H "Mcp-Session-Id: $SID" -X POST $MCP -d '{"jsonrpc":"2.0","method":"notifications/initialized"}'
   ```

3. Tool aufrufen (`processId` aus der Antwort des Uploads `POST /api/v1/ingest/documents`):

   ```bash
   curl -s "${HDR[@]}" -H "Mcp-Session-Id: $SID" -X POST $MCP -d '{"jsonrpc":"2.0","id":2,"method":"tools/call",
     "params":{"name":"get_process_suggestions","arguments":{"processId":"<processId>"}}}'
   ```

   Antwort (SSE-Event `message`): `{"result":{"content":[{"type":"text","text":"{\"processId\":…,\"documentTypeId\":…,\"attributes\":[…]}"}],"isError":false}}`.
   Fremde oder unbekannte `processId` → `isError: true`, `code: not_found`. Alternativ mit dem
   MCP Inspector (`npx @modelcontextprotocol/inspector`, Transport „Streamable HTTP", URL `$MCP`,
   Header `Authorization`).

## Qualitätssicherung

Die Teststrategie umfasst:

- Unit-Tests für Domänenlogik und Validierung
- Integrationstests für docling, pgvector, Ollama-Mocks und DMS-Adapter
- Tests für Tokenizer-Alignment und Chunk-Kontextualisierung
- Fehlerinjektion für ungültige LLM-Ausgaben, docling-Abstürze und OCR-Probleme
- Cross-Tenant- und Cross-ACL-Tests
- Tests für SSE-Reconnect und Job-Recovery
- Nachweise zur Datenminimierung und Embedding-Quarantäne
- ArchUnit-Tests für Modulgrenzen
- Smoke-, Egress- und Security-Checks in der CI-Pipeline

Die messbaren Qualitätsziele und Evaluationsfälle sind in [doc/SPEC.md](doc/SPEC.md) definiert.

## Dokumentation

- [doc/SPEC.md](doc/SPEC.md): Ziele, Scope, Anforderungen, Qualitätsziele und Guardrails
- [doc/ARCHITECTURE.md](doc/ARCHITECTURE.md): Architektur, Laufzeitsichten, Datenmodell und ADRs
- [.github/copilot-instructions.md](.github/copilot-instructions.md): Verbindliche Arbeitsanweisungen für GitHub Copilot
- [doc/PROJEKTPLAN.md](doc/PROJEKTPLAN.md): Block- und Meilensteinplanung, sofern im Repository vorhanden

Bei Widersprüchen gelten `doc/SPEC.md` und `doc/ARCHITECTURE.md` als verbindliche Quellen.

## Projektstatus

Die Umsetzung erfolgt schrittweise entlang der in der Spezifikation definierten Meilensteine:

- **M1:** Lauffähiges Gerüst
- **M1.5:** Durchgängiger Extraktionspfad
- **M2:** Erste End-to-End-Messung
- **M3:** Härtung und belastbare Compliance-Nachweise

## Lizenz

Copyright © 2026 Jonas Segessemann. Alle Rechte vorbehalten.

Der Quellcode wird im Rahmen einer Projektarbeit öffentlich zur
Einsichtnahme bereitgestellt. Die Veröffentlichung stellt keine
Open-Source-Lizenz dar.

Nutzung, Vervielfältigung, Veränderung, Weitergabe oder kommerzielle
Verwertung sind ohne vorherige schriftliche Zustimmung des
Rechteinhabers nicht gestattet.

Die verbindlichen Lizenzbedingungen befinden sich in der Datei
LICENSE.
