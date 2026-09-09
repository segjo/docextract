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

Weitere Details befinden sich in [ARCHITECTURE.md](ARCHITECTURE.md).

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

### Lokale Entwicklung (dev mode)

1. d.velop-Mock (DMS/IdP-Simulation) starten:

   ```bash
   docker compose up dvelop-mock
   ```

2. Anwendung bauen und starten (Profil `local`):

   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

3. Aufruf testen:

   ```bash
   curl -X GET "http://localhost:8080/adeon-docextract/api/v1/ingest/test" \
     -H "Authorization: Bearer dummy-token"
   ```

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

Die messbaren Qualitätsziele und Evaluationsfälle sind in [SPEC.md](SPEC.md) definiert.

## Dokumentation

- [SPEC.md](SPEC.md): Ziele, Scope, Anforderungen, Qualitätsziele und Guardrails
- [ARCHITECTURE.md](ARCHITECTURE.md): Architektur, Laufzeitsichten, Datenmodell und ADRs
- [copilot-instructions.md](copilot-instructions.md): Verbindliche Arbeitsanweisungen für GitHub Copilot
- [PROJEKTPLAN.md](PROJEKTPLAN.md): Block- und Meilensteinplanung, sofern im Repository vorhanden

Bei Widersprüchen gelten `SPEC.md` und `ARCHITECTURE.md` als verbindliche Quellen.

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
