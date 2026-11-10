# Repo-weite Änderung P7 — Einheitliche Fehlerform (ProblemDetail)

Prüfliste für den Diff-Review (Block 3). Spalten **Entscheid** und **Grund** füllt der Mensch nach dem Review
(`übernommen` / `korrigiert` / `verworfen`). Die Änderung liegt in einem separaten Commit; Diff je Datei:
`git show <commit> -- <Datei>`.

| Datei | Änderung (1 Satz) | Entscheid: übernommen/korrigiert/verworfen | Grund | Beleg (Test/Commit) |
| --- | --- | --- | --- | --- |
| `doc/ARCHITECTURE.md` | § 8.1 um Fehlerform und Code-Tabelle, § 6.5 (413/422), § 5.5/5.6 (shared.error, keine veröffentlichten Domain-Typen) und MCP-Fehlerformat (§ 6.3) ergänzt. |  |  | – |
| `doc/ENTWICKLUNGSTAGEBUCH.md` | Eintrag P7 (Prompt, Dateien, Generiertes, OFFEN-Punkte). |  |  | – |
| `doc/api/openapi.yaml` | Aus den Controllern neu generiert: 416/422/502 ergänzt, `Problem` mit `processId` und urn-Typen, P7-Hinweise entfernt. |  |  | OpenApiContractTest |
| `src/main/java/ch/adeon/apps/docextract/agentgateway/adapter/in/mcp/McpToolAdapter.java` | Tool-Fehler mit `code`/`type`/`title`/`detail`/`processId` aus `ProblemCode`; unerwartete Fehler ohne Meldung. |  |  | McpToolAdapterTest |
| `src/main/java/ch/adeon/apps/docextract/agentgateway/application/AgentGatewayService.java` | Wirft shared-Exceptions statt `AgentToolException`; Audit-Outcomes unverändert. |  |  | AgentGatewayServiceTest |
| `src/main/java/ch/adeon/apps/docextract/agentgateway/application/AgentToolException.java` | Gelöscht; der Gateway wirft jetzt `InvalidRequestException` bzw. `ProcessNotFoundException`. |  |  | AgentGatewayServiceTest |
| `src/main/java/ch/adeon/apps/docextract/content/adapter/out/docling/DoclingDocumentContentAdapter.java` | Nur Import angepasst (Exception nach `shared.error` verschoben). |  |  | Kompilierung, mvn verify |
| `src/main/java/ch/adeon/apps/docextract/content/adapter/out/pdfbox/PdfBoxDocumentContentAdapter.java` | Nur Import angepasst (Exception nach `shared.error` verschoben). |  |  | Kompilierung, mvn verify |
| `src/main/java/ch/adeon/apps/docextract/content/adapter/out/pdfbox/PdfBoxTextExtractionAdapter.java` | Nur Import angepasst (Exception nach `shared.error` verschoben). |  |  | Kompilierung, mvn verify |
| `src/main/java/ch/adeon/apps/docextract/content/application/ContentService.java` | Nur Import angepasst (Exception nach `shared.error` verschoben). |  |  | Kompilierung, mvn verify |
| `src/main/java/ch/adeon/apps/docextract/content/application/ExtractTextService.java` | Nur Import angepasst (Exception nach `shared.error` verschoben). |  |  | Kompilierung, mvn verify |
| `src/main/java/ch/adeon/apps/docextract/extraction/application/ExtractAttributesService.java` | Nur Import angepasst (Exception nach `shared.error` verschoben). |  |  | Kompilierung, mvn verify |
| `src/main/java/ch/adeon/apps/docextract/ingest/adapter/in/rest/ImportInfoController.java` | `502` (BadGateway-Referenz) dokumentiert. |  |  | OpenApiContractTest |
| `src/main/java/ch/adeon/apps/docextract/ingest/adapter/in/rest/PreviewController.java` | Range-Parsing: ungültig/Suffix/mehrfach wird ignoriert; 416 dokumentiert. |  |  | PreviewControllerRangeTest |
| `src/main/java/ch/adeon/apps/docextract/ingest/adapter/in/rest/UploadController.java` | OpenAPI-Texte für 400/413 angepasst, 422 (Seitenzahl) ergänzt. |  |  | OpenApiContractTest |
| `src/main/java/ch/adeon/apps/docextract/ingest/application/BlobNotFoundException.java` | Erbt von `NotFoundException`. |  |  | GlobalExceptionHandlerTest |
| `src/main/java/ch/adeon/apps/docextract/ingest/application/DmsChunkUploadException.java` | Erbt von `AdapterUnavailableException`. |  |  | GlobalExceptionHandlerTest |
| `src/main/java/ch/adeon/apps/docextract/ingest/application/DmsRepositoryException.java` | Erbt von `AdapterUnavailableException`. |  |  | GlobalExceptionHandlerTest |
| `src/main/java/ch/adeon/apps/docextract/ingest/application/IngestDocumentService.java` | Dateigrösse wirft `LimitExceededException` (413) statt `UploadRejectedException`. |  |  | IngestDocumentServiceTest |
| `src/main/java/ch/adeon/apps/docextract/ingest/application/PreviewRenderingException.java` | Erbt von `AdapterUnavailableException`. |  |  | GlobalExceptionHandlerTest |
| `src/main/java/ch/adeon/apps/docextract/ingest/application/PreviewStreamService.java` | Range hinter dem Dokumentende wirft `RangeNotSatisfiableException`. |  |  | PreviewStreamServiceTest |
| `src/main/java/ch/adeon/apps/docextract/ingest/application/UploadRejectedException.java` | Erbt von `InvalidRequestException`. |  |  | GlobalExceptionHandlerTest |
| `src/main/java/ch/adeon/apps/docextract/ingest/domain/Limits.java` | Zu grosse Datei als `tooLarge` markiert. |  |  | LimitsTest |
| `src/main/java/ch/adeon/apps/docextract/ingest/domain/ValidationResult.java` | Flag `sizeExceeded` ergänzt. |  |  | LimitsTest |
| `src/main/java/ch/adeon/apps/docextract/process/application/ProcessNotFoundException.java` | Erbt von `NotFoundException`, trägt `processId`. |  |  | GlobalExceptionHandlerTest |
| `src/main/java/ch/adeon/apps/docextract/retrieval/adapter/in/rest/RetrievalController.java` | `502` für object-definitions und value-lists dokumentiert. |  |  | OpenApiContractTest |
| `src/main/java/ch/adeon/apps/docextract/retrieval/adapter/out/dms/DmsObjectDefinitionException.java` | Erbt von `AdapterUnavailableException`. |  |  | GlobalExceptionHandlerTest |
| `src/main/java/ch/adeon/apps/docextract/retrieval/adapter/out/dms/DmsObjectMetadataException.java` | Erbt von `AdapterUnavailableException`. |  |  | GlobalExceptionHandlerTest |
| `src/main/java/ch/adeon/apps/docextract/retrieval/adapter/out/dms/ValueListWebhookException.java` | Erbt von `AdapterUnavailableException`. |  |  | GlobalExceptionHandlerTest |
| `src/main/java/ch/adeon/apps/docextract/retrieval/application/FindSimilarService.java` | Nur Import angepasst (Exception nach `shared.error` verschoben). |  |  | Kompilierung, mvn verify |
| `src/main/java/ch/adeon/apps/docextract/retrieval/application/StagePendingEmbeddingsService.java` | Nur Import angepasst (Exception nach `shared.error` verschoben). |  |  | Kompilierung, mvn verify |
| `src/main/java/ch/adeon/apps/docextract/security/adapter/in/web/DvelopAuthenticationFilter.java` | 403-Body mit `urn:docextract:problem:forbidden`. |  |  | TenantResolutionTest |
| `src/main/java/ch/adeon/apps/docextract/security/adapter/in/web/SecurityConfig.java` | 401/403 mit `type` aus `ProblemCode` via `ProblemDetails`. |  |  | TenantResolutionTest, McpEndpointSecurityTest |
| `src/main/java/ch/adeon/apps/docextract/shared/adapter/web/GlobalExceptionHandler.java` | Neu (aus `shared.web` verschoben): ein `@ControllerAdvice` auf `ResponseEntityExceptionHandler` mit allen Abbildungen, 416-Header, Fallback 500, SPA-Ausnahme. |  |  | GlobalExceptionHandlerTest |
| `src/main/java/ch/adeon/apps/docextract/shared/adapter/web/ProblemDetails.java` | Neu: Hilfsklasse für ProblemDetail mit `type`/`title` aus `ProblemCode`. |  |  | GlobalExceptionHandlerTest |
| `src/main/java/ch/adeon/apps/docextract/shared/config/OpenApiConfig.java` | Schema `Problem` (processId, urn-Typen), Response `BadGateway`, P7-Hinweise entfernt. |  |  | OpenApiContractTest |
| `src/main/java/ch/adeon/apps/docextract/shared/error/AdapterUnavailableException.java` | Neu: 502 mit festem `detail`. |  |  | GlobalExceptionHandlerTest |
| `src/main/java/ch/adeon/apps/docextract/shared/error/ConflictStateException.java` | Neu: 409. |  |  | GlobalExceptionHandlerTest |
| `src/main/java/ch/adeon/apps/docextract/shared/error/ContentException.java` | Nach `shared.error` verschoben, erbt von `ProcessingFailedException`. |  |  | GlobalExceptionHandlerTest, HexagonalArchitectureTest |
| `src/main/java/ch/adeon/apps/docextract/shared/error/DocextractException.java` | Neu: Basis aller gemeldeten Fehler (`code`, PII-freies `detail`, `processId`). |  |  | GlobalExceptionHandlerTest |
| `src/main/java/ch/adeon/apps/docextract/shared/error/ExtractionException.java` | Nach `shared.error` verschoben, erbt von `ProcessingFailedException`. |  |  | GlobalExceptionHandlerTest, HexagonalArchitectureTest |
| `src/main/java/ch/adeon/apps/docextract/shared/error/InvalidRequestException.java` | Neu: 400. |  |  | GlobalExceptionHandlerTest |
| `src/main/java/ch/adeon/apps/docextract/shared/error/LimitExceededException.java` | Neu: 413 (Dateigrösse) bzw. 422 (Seitenzahl) über `Limit`. |  |  | GlobalExceptionHandlerTest, IngestDocumentServiceTest |
| `src/main/java/ch/adeon/apps/docextract/shared/error/NotFoundException.java` | Neu: 404 (kein eigener Tenant-Mismatch-Typ, NfA-4). |  |  | GlobalExceptionHandlerTest |
| `src/main/java/ch/adeon/apps/docextract/shared/error/ProblemCode.java` | Neu: frameworkfreie Fehlercodes (slug, title, type-URI). |  |  | GlobalExceptionHandlerTest, McpToolAdapterTest |
| `src/main/java/ch/adeon/apps/docextract/shared/error/ProcessingFailedException.java` | Neu: 500 mit festem `detail`. |  |  | GlobalExceptionHandlerTest |
| `src/main/java/ch/adeon/apps/docextract/shared/error/RangeNotSatisfiableException.java` | Neu: 416 mit Gesamtlänge. |  |  | GlobalExceptionHandlerTest, PreviewStreamServiceTest |
| `src/main/java/ch/adeon/apps/docextract/shared/error/RetrievalException.java` | Nach `shared.error` verschoben, erbt von `ProcessingFailedException`. |  |  | GlobalExceptionHandlerTest, HexagonalArchitectureTest |
| `src/main/java/ch/adeon/apps/docextract/validation/application/AlreadyFinalizedException.java` | Erbt von `ConflictStateException`. |  |  | GlobalExceptionHandlerTest |
| `src/main/java/ch/adeon/apps/docextract/validation/application/InvalidAttributesException.java` | Erbt von `DocextractException`; `detail` fest, DMS-Grund nur in der Meldung (Log). |  |  | GlobalExceptionHandlerTest |
| `src/main/java/ch/adeon/apps/docextract/validation/application/PossiblyAlreadyFinalizedException.java` | Erbt von `ConflictStateException`. |  |  | GlobalExceptionHandlerTest |
| `src/main/java/ch/adeon/apps/docextract/validation/application/ProcessExpiredException.java` | Erbt von `DocextractException` (`process-expired`, 410). |  |  | GlobalExceptionHandlerTest |
| `src/main/java/ch/adeon/apps/docextract/validation/port/DmsWriteFailedException.java` | Erbt von `AdapterUnavailableException`. |  |  | GlobalExceptionHandlerTest |
| `src/test/java/ch/adeon/apps/docextract/agentgateway/adapter/in/mcp/McpEndpointSecurityTest.java` | Erwartet `invalid-request` statt `invalid_argument`. |  |  | McpEndpointSecurityTest |
| `src/test/java/ch/adeon/apps/docextract/agentgateway/adapter/in/mcp/McpToolAdapterTest.java` | Neue Codes; Tests für gleiche Codes wie REST und für nicht durchsickernde Meldungen. |  |  | McpToolAdapterTest |
| `src/test/java/ch/adeon/apps/docextract/agentgateway/application/AgentGatewayServiceTest.java` | Erwartet shared-Exceptions. |  |  | AgentGatewayServiceTest |
| `src/test/java/ch/adeon/apps/docextract/architecture/HexagonalArchitectureTest.java` | Regel ohne `PUBLISHED_DOMAIN_TYPES` (keine Ausnahmen mehr), umbenannt. |  |  | HexagonalArchitectureTest |
| `src/test/java/ch/adeon/apps/docextract/extraction/application/ExtractAttributesServiceTest.java` | Nur Import angepasst (Exception nach `shared.error` verschoben). |  |  | Kompilierung, mvn verify |
| `src/test/java/ch/adeon/apps/docextract/ingest/adapter/in/rest/PreviewControllerRangeTest.java` | Neu: Range-Parsing (gültig, ungültig, Suffix, mehrfach). |  |  | PreviewControllerRangeTest |
| `src/test/java/ch/adeon/apps/docextract/ingest/application/IngestDocumentServiceTest.java` | Zu grosse Datei → `LimitExceededException`. |  |  | IngestDocumentServiceTest |
| `src/test/java/ch/adeon/apps/docextract/ingest/application/PreviewStreamServiceTest.java` | Test für nicht erfüllbare Range. |  |  | PreviewStreamServiceTest |
| `src/test/java/ch/adeon/apps/docextract/ingest/domain/LimitsTest.java` | Prüft `sizeExceeded`. |  |  | LimitsTest |
| `src/test/java/ch/adeon/apps/docextract/retrieval/application/FindSimilarServiceTest.java` | Nur Import angepasst (Exception nach `shared.error` verschoben). |  |  | Kompilierung, mvn verify |
| `src/test/java/ch/adeon/apps/docextract/retrieval/application/StagePendingEmbeddingsServiceTest.java` | Nur Import angepasst (Exception nach `shared.error` verschoben). |  |  | Kompilierung, mvn verify |
| `src/test/java/ch/adeon/apps/docextract/shared/adapter/web/GlobalExceptionHandlerTest.java` | Neu: Status + type je Fehlerklasse, PII-Freiheit von `detail`, Framework-Fehler, 413/416. |  |  | GlobalExceptionHandlerTest |
| `src/test/java/ch/adeon/apps/docextract/shared/web/TenantScopedEndpointsTest.java` | Import des verschobenen Handlers. |  |  | TenantScopedEndpointsTest |
| `src/test/java/ch/adeon/apps/docextract/validation/adapter/in/rest/ValidationControllerIdempotencyTest.java` | Import des verschobenen Handlers. |  |  | ValidationControllerIdempotencyTest |
| `doc/block3/repo-wide-change.md` | Neu: diese Prüfliste. |  |  | – |
| `src/main/java/ch/adeon/apps/docextract/content/domain/ContentException.java` | Alter Pfad: nach `shared/error` verschoben (Rename). |  |  | HexagonalArchitectureTest |
| `src/main/java/ch/adeon/apps/docextract/extraction/domain/ExtractionException.java` | Alter Pfad: nach `shared/error` verschoben (Rename). |  |  | HexagonalArchitectureTest |
| `src/main/java/ch/adeon/apps/docextract/retrieval/domain/RetrievalException.java` | Alter Pfad: nach `shared/error` verschoben (Rename). |  |  | HexagonalArchitectureTest |
| `src/main/java/ch/adeon/apps/docextract/shared/web/GlobalExceptionHandler.java` | Alter Pfad gelöscht; Ersatz in `shared/adapter/web`. |  |  | GlobalExceptionHandlerTest |
