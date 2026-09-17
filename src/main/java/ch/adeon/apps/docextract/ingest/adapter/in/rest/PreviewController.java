package ch.adeon.apps.docextract.ingest.adapter.in.rest;

import ch.adeon.apps.docextract.ingest.application.PreviewChunk;
import ch.adeon.apps.docextract.ingest.application.StreamPreview;
import ch.adeon.apps.docextract.ingest.domain.PageRange;
import java.util.UUID;
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
public class PreviewController {

  private final StreamPreview streamPreview;

  public PreviewController(StreamPreview streamPreview) {
    this.streamPreview = streamPreview;
  }

  @GetMapping("/{blobId}/preview")
  public ResponseEntity<byte[]> preview(
      @PathVariable UUID blobId,
      @RequestHeader(value = HttpHeaders.RANGE, required = false) String range) {
    PageRange requestedRange = range == null ? null : parseRange(range);
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

  /** Parses the standard single-range {@code bytes=start-end} form of the HTTP Range header. */
  private static PageRange parseRange(String header) {
    String spec = header.replaceFirst("(?i)^bytes=", "");
    String[] bounds = spec.split("-", 2);
    long start = bounds[0].isBlank() ? 0 : Long.parseLong(bounds[0]);
    long end =
        (bounds.length > 1 && !bounds[1].isBlank()) ? Long.parseLong(bounds[1]) : Long.MAX_VALUE;
    return new PageRange(start, end);
  }
}
