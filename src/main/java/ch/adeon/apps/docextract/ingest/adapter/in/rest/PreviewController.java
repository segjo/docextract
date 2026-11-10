package ch.adeon.apps.docextract.ingest.adapter.in.rest;

import static ch.adeon.apps.docextract.shared.config.OpenApiConfig.BAD_REQUEST_REF;
import static ch.adeon.apps.docextract.shared.config.OpenApiConfig.PROBLEM_JSON;
import static ch.adeon.apps.docextract.shared.config.OpenApiConfig.PROBLEM_SCHEMA_REF;
import static ch.adeon.apps.docextract.shared.config.OpenApiConfig.UNAUTHORIZED_REF;

import ch.adeon.apps.docextract.ingest.application.PreviewChunk;
import ch.adeon.apps.docextract.ingest.application.StreamPreview;
import ch.adeon.apps.docextract.ingest.port.PageRange;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * {@code GET .../{blobId}/preview} — Range-fähiges Streaming aus dem transienten
 * Postgres-Blobstore: native PDFs kommen direkt aus dem ORIGINAL-Blob, Nicht-PDFs aus dem lazily
 * gerenderten PREVIEW-Blob (ADR-008).
 */
@RestController
@RequestMapping("/api/v1/ingest/documents")
@Tag(name = "ingest")
public class PreviewController {

  private static final Pattern SINGLE_RANGE = Pattern.compile("(?i)bytes=(\\d{1,18})-(\\d{0,18})");

  private final StreamPreview streamPreview;

  public PreviewController(StreamPreview streamPreview) {
    this.streamPreview = streamPreview;
  }

  @GetMapping("/{blobId}/preview")
  @Operation(
      operationId = "preview",
      summary = "PDF-Vorschau streamen (Range-fähig)",
      description =
          "Native PDFs aus dem ORIGINAL-Blob, Nicht-PDFs aus dem gerenderten PREVIEW-Blob"
              + " (ADR-008). Unterstützt eine einzelne Byte-Range `bytes=start-end`.")
  @ApiResponse(
      responseCode = "200",
      description = "Ganzes Dokument.",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PDF_VALUE,
              schema = @Schema(type = "string", format = "binary")))
  @ApiResponse(
      responseCode = "206",
      description = "Teilinhalt gemäss Range-Header.",
      headers =
          @Header(
              name = HttpHeaders.CONTENT_RANGE,
              description = "bytes start-end/total",
              schema = @Schema(type = "string")),
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PDF_VALUE,
              schema = @Schema(type = "string", format = "binary")))
  @ApiResponse(responseCode = "400", ref = BAD_REQUEST_REF)
  @ApiResponse(responseCode = "401", ref = UNAUTHORIZED_REF)
  @ApiResponse(
      responseCode = "416",
      description =
          "Range beginnt hinter dem Dokumentende. Header `Content-Range: bytes */<Länge>`."
              + " Syntaktisch ungültige Ranges werden ignoriert (200, ganzer Inhalt).",
      headers =
          @Header(
              name = HttpHeaders.CONTENT_RANGE,
              description = "bytes */total",
              schema = @Schema(type = "string")),
      content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
  @ApiResponse(
      responseCode = "404",
      description = "Blob unbekannt, abgelaufen oder gehört einem anderen Tenant (NfA-4).",
      content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
  @ApiResponse(
      responseCode = "502",
      description = "Rendern der Vorschau (Nicht-PDF) fehlgeschlagen.",
      content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_SCHEMA_REF)))
  public ResponseEntity<byte[]> preview(
      @PathVariable UUID blobId,
      @Parameter(description = "Einzelne Byte-Range, z. B. `bytes=0-1048575`.")
          @RequestHeader(value = HttpHeaders.RANGE, required = false)
          String range) {
    PageRange requestedRange = parseRange(range);
    PreviewChunk chunk = streamPreview.stream(blobId, requestedRange);

    ResponseEntity.BodyBuilder response;
    if (requestedRange == null) {
      response = ResponseEntity.ok();
    } else {
      long endInclusive = requestedRange.start() + chunk.content().length - 1;
      response =
          ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
              .header(
                  HttpHeaders.CONTENT_RANGE,
                  "bytes %d-%d/%d"
                      .formatted(requestedRange.start(), endInclusive, chunk.totalSize()));
    }
    return response.contentType(MediaType.APPLICATION_PDF).body(chunk.content());
  }

  /**
   * Parses the single-range form {@code bytes=start[-end]}. Everything else (garbage, {@code
   * end < start}, suffix ranges, several ranges) yields {@code null}, i.e. the header is ignored and
   * the full content is served (RFC 9110 § 14.2: a server may ignore Range).
   */
  static PageRange parseRange(String header) {
    if (header == null) {
      return null;
    }
    Matcher matcher = SINGLE_RANGE.matcher(header.strip());
    if (!matcher.matches()) {
      return null;
    }
    long start = Long.parseLong(matcher.group(1));
    long end = matcher.group(2).isEmpty() ? Long.MAX_VALUE : Long.parseLong(matcher.group(2));
    return end < start ? null : new PageRange(start, end);
  }
}
