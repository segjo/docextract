package ch.adeon.apps.docextract.ingest.adapter.in.rest;

import static ch.adeon.apps.docextract.shared.config.OpenApiConfig.PROBLEM_JSON;
import static ch.adeon.apps.docextract.shared.config.OpenApiConfig.PROBLEM_SCHEMA_REF;
import static ch.adeon.apps.docextract.shared.config.OpenApiConfig.UNAUTHORIZED_REF;

import ch.adeon.apps.docextract.ingest.application.IngestCommand;
import ch.adeon.apps.docextract.ingest.application.IngestDocument;
import ch.adeon.apps.docextract.ingest.application.IngestedDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
/**
 * {@code POST /documents} — synchronous blob-store write + original-chunk-upload, then {@code 202
 * Accepted + processId} (FR-1, ADR-008). The actual DMS object is only finalized later, after human
 * consent (validation module).
 */
@RestController
@RequestMapping("/api/v1/ingest/documents")
@Tag(name = "ingest", description = "Upload, Vorschau, Importmaske (FR-1, ADR-008)")
public class UploadController {

  private final IngestDocument ingestDocument;

  public UploadController(IngestDocument ingestDocument) {
    this.ingestDocument = ingestDocument;
  }

  @PostMapping(consumes = "multipart/form-data")
  @Operation(
      operationId = "upload",
      summary = "Dokument hochladen und Pipeline asynchron starten",
      description =
          "Speichert die Bytes TTL-begrenzt im Blobstore und lädt das Original write-only ins DMS"
              + " (Chunk-Upload). Fortschritt über GET /api/v1/processes/{processId}/events.")
  @ApiResponse(
      responseCode = "202",
      description = "Angenommen; Verarbeitung läuft asynchron (ADR-004).",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = IngestedDocument.class)))
  @ApiResponse(
      responseCode = "400",
      description =
          "Upload abgewiesen (leere Datei, nicht unterstützter Medientyp; T-4) oder Parameter"
              + " file/repositoryId fehlt.",
      content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
  @ApiResponse(responseCode = "401", ref = UNAUTHORIZED_REF)
  @ApiResponse(
      responseCode = "413",
      description =
          "Datei grösser als das Limit (docextract.ingest.max-file-size-bytes) oder Request"
              + " grösser als spring.servlet.multipart.max-request-size (50 MB); gleicher type"
              + " `limit-exceeded`.",
      content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
  @ApiResponse(
      responseCode = "502",
      description = "DMS-Chunk-Upload fehlgeschlagen.",
      content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
  public ResponseEntity<IngestedDocument> upload(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "repositoryId", required = true) String repositoryId)
      throws IOException {
    IngestCommand command =
        IngestCommand.of(
            file.getOriginalFilename(), file.getContentType(), file.getBytes(), repositoryId);
    IngestedDocument ingested = ingestDocument.ingest(command);
    return ResponseEntity.accepted().body(ingested);
  }
}
