package ch.adeon.apps.docextract.ingest.adapter.out.preview;

import ch.adeon.apps.docextract.ingest.application.PreviewRenderPort;
import ch.adeon.apps.docextract.ingest.application.PreviewRenderingException;
import ch.adeon.apps.docextract.ingest.domain.MediaType;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Renders a non-PDF upload into a PDF via Gotenberg's LibreOffice route ({@code POST
 * /forms/libreoffice/convert}). Consumes bytes read from the ORIGINAL blob and returns the PDF for
 * storage as the PREVIEW blob (ADR-008).
 */
@Component
public class GotenbergPreviewAdapter implements PreviewRenderPort {

  private final RestClient restClient;
  private final String baseUri;

  public GotenbergPreviewAdapter(@Value("${docextract.gotenberg.base-uri}") String baseUri) {
    this.restClient = RestClient.create();
    this.baseUri = baseUri;
  }

  /**
   * Media types this adapter renders via Gotenberg's LibreOffice route — a deliberately curated
   * subset of business-document formats LibreOffice can convert (LibreOffice itself supports far
   * more, e.g. {@code .epub}, {@code .pages}, vector/drawing formats; widening this needs a
   * deliberate decision, not just "LibreOffice happens to support it", T-4 attack-surface
   * minimization). Owned here, not shared with other adapters — each adapter's supported set
   * reflects only what that specific tool actually does.
   */
  private static final Map<String, String> EXTENSION_BY_MEDIA_TYPE =
      Map.ofEntries(
          Map.entry("application/msword", "doc"),
          Map.entry(
              "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
          Map.entry("application/vnd.ms-excel", "xls"),
          Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
          Map.entry("application/vnd.ms-powerpoint", "ppt"),
          Map.entry(
              "application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx"),
          Map.entry("application/vnd.oasis.opendocument.text", "odt"),
          Map.entry("application/vnd.oasis.opendocument.spreadsheet", "ods"),
          Map.entry("application/vnd.oasis.opendocument.presentation", "odp"),
          Map.entry("application/rtf", "rtf"),
          Map.entry("text/rtf", "rtf"),
          Map.entry("text/csv", "csv"),
          Map.entry("text/plain", "txt"),
          Map.entry("text/html", "html"),
          Map.entry("image/png", "png"),
          Map.entry("image/jpeg", "jpg"),
          Map.entry("image/gif", "gif"),
          Map.entry("image/bmp", "bmp"),
          Map.entry("image/tiff", "tiff"));

  @Override
  public byte[] renderPreview(String filename, MediaType mediaType, byte[] content) {
    String namedFilename = withExtension(filename, mediaType);
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("files", new NamedByteArrayResource(content, namedFilename));
    try {
      byte[] pdf =
          restClient
              .post()
              .uri(baseUri + "/forms/libreoffice/convert")
              .body(body)
              .retrieve()
              .body(byte[].class);
      if (pdf == null || pdf.length == 0) {
        throw new PreviewRenderingException("Gotenberg returned an empty PDF for " + filename);
      }
      return pdf;
    } catch (RestClientException ex) {
      throw new PreviewRenderingException("Gotenberg conversion failed for " + filename, ex);
    }
  }

  @Override
  public boolean supports(MediaType mediaType) {
    return EXTENSION_BY_MEDIA_TYPE.containsKey(mediaType.value().toLowerCase(Locale.ROOT));
  }

  /**
   * Gotenberg's LibreOffice route selects the input format from the filename extension, not the
   * MIME type. Callers here pass the blob id (no extension), so one is appended from the MIME type
   * before upload.
   */
  private static String withExtension(String filename, MediaType mediaType) {
    if (filename != null && filename.lastIndexOf('.') > 0) {
      return filename;
    }
    String extension = EXTENSION_BY_MEDIA_TYPE.get(mediaType.value().toLowerCase(Locale.ROOT));
    if (extension == null) {
      throw new PreviewRenderingException(
          "Unsupported media type for preview rendering: " + mediaType.value());
    }
    return filename + "." + extension;
  }

  /** Gives the multipart "files" part a filename, required by Gotenberg's form parser. */
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
