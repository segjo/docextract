package ch.adeon.apps.docextract.structuring.adapter.out.docling;

import ch.adeon.apps.docextract.ingest.domain.MediaType;
import ch.adeon.apps.docextract.shared.http.Http1RestClientFactory;
import ch.adeon.apps.docextract.structuring.application.StructuringPort;
import ch.adeon.apps.docextract.structuring.domain.StructuredContent;
import ch.adeon.apps.docextract.structuring.domain.StructuringException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Primary structuring adapter (ADR-007): converts the document via docling-serve's synchronous
 * {@code POST /v1/convert/file}, requesting Markdown output so headings survive for the
 * heading-aware chunker. Left as a thin conversion step deliberately — the real docling {@code
 * HybridChunker}/{@code contextualize()} chunk-level output (docling-serve's {@code
 * to_formats=["chunks"]}) can replace {@link
 * ch.adeon.apps.docextract.structuring.domain.HeadingAwareChunker} later without touching this
 * port's contract.
 */
@Component
@ConditionalOnProperty(
    name = "docextract.structuring.mode",
    havingValue = "docling",
    matchIfMissing = true)
public class DoclingStructuringAdapter implements StructuringPort {

  private final RestClient restClient;
  private final String baseUri;

  /**
   * Media types this adapter converts — a deliberately curated subset of docling's real input
   * format support (docling also handles Markdown/AsciiDoc/LaTeX, MHTML, audio/video via the ASR
   * extra, email, BoxNote, AFP, ...; those are out of scope for incoming business documents here,
   * ADR-007). Owned here, not shared with {@code GotenbergPreviewAdapter} — the two tools' actual
   * capabilities differ (e.g. docling has no native GIF support, unlike LibreOffice).
   */
  private static final Set<String> SUPPORTED_MEDIA_TYPES =
      Set.of(
          "application/pdf",
          "application/msword",
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
          "application/vnd.ms-excel",
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
          "application/vnd.ms-powerpoint",
          "application/vnd.openxmlformats-officedocument.presentationml.presentation",
          "application/vnd.oasis.opendocument.text",
          "application/vnd.oasis.opendocument.spreadsheet",
          "application/vnd.oasis.opendocument.presentation",
          "application/rtf",
          "text/rtf",
          "text/csv",
          "text/html",
          "image/png",
          "image/jpeg",
          "image/tiff",
          "image/bmp",
          "image/webp");

  public DoclingStructuringAdapter(@Value("${docextract.docling.base-uri}") String baseUri) {
    this.restClient = Http1RestClientFactory.create();
    this.baseUri = baseUri;
  }

  @Override
  public StructuredContent convert(byte[] content, MediaType mediaType) {
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("files", new NamedByteArrayResource(content, "document" + extensionFor(mediaType)));
    body.add("to_formats", "md");
    try {
      ConvertResponse response =
          restClient
              .post()
              .uri(baseUri + "/v1/convert/file")
              .body(body)
              .retrieve()
              .body(ConvertResponse.class);
      if (response == null || response.document() == null) {
        throw new StructuringException("docling returned no document content");
      }
      if ("failure".equals(response.status())) {
        throw new StructuringException("docling conversion failed: " + response.errors());
      }
      // An empty result (e.g. a blank page) is a valid outcome, not an error (E-5: "unbekannt"
      // beats a spurious failure) — it simply yields zero chunks downstream.
      String markdown = response.document().mdContent();
      return new StructuredContent(markdown == null ? "" : markdown);
    } catch (RestClientException ex) {
      throw new StructuringException(
          "docling conversion failed at " + baseUri + ": " + ex.getMessage(), ex);
    }
  }

  private static String extensionFor(MediaType mediaType) {
    return mediaType.isPdf() ? ".pdf" : "";
  }

  @Override
  public boolean supports(MediaType mediaType) {
    return SUPPORTED_MEDIA_TYPES.contains(mediaType.value().toLowerCase(Locale.ROOT));
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record ConvertResponse(
      DoclingDocument document, String status, java.util.List<String> errors) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record DoclingDocument(@JsonProperty("md_content") String mdContent) {}

  /** Gives the multipart "files" part a filename, required by docling-serve's form parser. */
  private static final class NamedByteArrayResource extends ByteArrayResource {

    private final String filename;

    NamedByteArrayResource(byte[] byteArray, String filename) {
      super(byteArray);
      this.filename = filename;
    }

    @Override
    public String getFilename() {
      return filename;
    }
  }
}
