package ch.adeon.apps.docextract.ingest.domain;

import java.util.Locale;

/**
 * Content type of an uploaded document. Governs the Preview-Branch decision (ADR-008): PDF is
 * streamed as-is from the ORIGINAL blob, anything else is rendered via Gotenberg into a PREVIEW
 * blob.
 */
public record MediaType(String value) {

  public static final MediaType PDF = new MediaType("application/pdf");
  private static final MediaType OCTET_STREAM = new MediaType("application/octet-stream");

  public MediaType {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("media type must not be blank");
    }
  }

  public static MediaType of(String value) {
    return new MediaType(value);
  }

  /** Fallback used when a caller (e.g. the MCP tool) does not supply a content type. */
  public static MediaType guessFromFilename(String filename) {
    if (filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
      return PDF;
    }
    return OCTET_STREAM;
  }

  public boolean isPdf() {
    return PDF.value().equalsIgnoreCase(value);
  }
}
