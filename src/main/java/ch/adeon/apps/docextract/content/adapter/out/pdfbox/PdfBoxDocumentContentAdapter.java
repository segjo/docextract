package ch.adeon.apps.docextract.content.adapter.out.pdfbox;

import ch.adeon.apps.docextract.content.domain.ContentException;
import ch.adeon.apps.docextract.content.domain.DocumentContent;
import ch.adeon.apps.docextract.content.domain.Representation;
import ch.adeon.apps.docextract.content.port.DocumentContentPort;
import ch.adeon.apps.docextract.ingest.domain.MediaType;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Reference {@link DocumentContentPort} adapter (ADR-011, local): the full, untruncated PDFBox text
 * as the FR-4 document representation — unlike {@link PdfBoxTextExtractionAdapter}, no word limit.
 */
@Component
@ConditionalOnProperty(
    name = "docextract.adapters.document-content",
    havingValue = "fulltext",
    matchIfMissing = true)
public class PdfBoxDocumentContentAdapter implements DocumentContentPort {

  @Override
  public DocumentContent provide(byte[] content, MediaType mediaType) {
    if (!mediaType.isPdf()) {
      throw new ContentException(
          "PDFBox document content only supports PDF input, got " + mediaType.value());
    }
    try (PDDocument document = Loader.loadPDF(content)) {
      String text = new PDFTextStripper().getText(document);
      return new DocumentContent(Representation.FULLTEXT, text == null ? "" : text.strip());
    } catch (IOException ex) {
      throw new ContentException("PDFBox document content extraction failed", ex);
    }
  }

  @Override
  public boolean supports(MediaType mediaType) {
    return mediaType.isPdf();
  }

  @Override
  public Representation representation() {
    return Representation.FULLTEXT;
  }
}
