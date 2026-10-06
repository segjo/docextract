package ch.adeon.apps.docextract.content.adapter.out.docling;

import ch.adeon.apps.docextract.content.domain.ContentException;
import ch.adeon.apps.docextract.content.domain.DocumentContent;
import ch.adeon.apps.docextract.content.domain.Representation;
import ch.adeon.apps.docextract.content.port.DocumentContentPort;
import ch.adeon.apps.docextract.ingest.domain.MediaType;
import ch.adeon.apps.docextract.shared.http.Http1RestClientFactory;
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
 * Optional {@link DocumentContentPort} alternative (ADR-011/-012, local): converts the document via
 * docling-serve's {@code POST /v1/convert/file} into Markdown, giving the extraction LLM heading
 * structure that plain text loses. Text-only — docling's native chunker/{@code HybridChunker} is
 * deliberately not used here (ADR-012 replaces chunking with one embedding per document).
 */
@Component
@ConditionalOnProperty(name = "docextract.adapters.document-content", havingValue = "markdown")
public class DoclingDocumentContentAdapter implements DocumentContentPort {

  private final RestClient restClient;
  private final String baseUri;

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

  public DoclingDocumentContentAdapter(@Value("${docextract.docling.base-uri}") String baseUri) {
    this.restClient = Http1RestClientFactory.create();
    this.baseUri = baseUri;
  }

  @Override
  public DocumentContent provide(byte[] content, MediaType mediaType) {
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
        throw new ContentException("docling returned no document content");
      }
      if ("failure".equals(response.status())) {
        throw new ContentException("docling conversion failed: " + response.errors());
      }
      String markdown = response.document().mdContent();
      return new DocumentContent(Representation.MARKDOWN, markdown == null ? "" : markdown);
    } catch (RestClientException ex) {
      throw new ContentException(
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

  @Override
  public Representation representation() {
    return Representation.MARKDOWN;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record ConvertResponse(
      DoclingDocument document, String status, java.util.List<String> errors) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record DoclingDocument(@JsonProperty("md_content") String mdContent) {}

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
