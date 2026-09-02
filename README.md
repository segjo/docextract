# docextract-3

KI-gestützte Dokumenterfassung & -verschlagwortung für das Dokumentenmanagement (d.velop documents).

## Architektur-Baseline

Dieses Repository enthält eine minimale Java-21/Spring-Boot-4-Baseline als modularen Monolithen mit hexagonaler Struktur unter `ch.adeon.apps.docextract`:

- `ingest`, `structuring`, `retrieval`, `extraction`, `validation`, `agentgateway`, `audit`, `security`
- Paketkonvention je Modul: `domain`, `application`, `adapter/*`
- REST- und MCP-Inbound teilen dieselben Application-Services

## Guardrails (im Code verankert)

- Consent-Gate vor Write-Back (`ConfirmAndWriteBackService`)
- ACL-Pre-Filter in der pgvector-Query (`PgvectorSimilaritySearchAdapter`)
- Zentrales REST-Fehlerformat via `ProblemDetail` (`GlobalExceptionHandler`)
- Domain bleibt framework-frei, Modulgrenzen via ArchUnit-Tests
