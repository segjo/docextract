package ch.adeon.apps.docextract.structuring.adapter.out.pdfbox;

import ch.adeon.apps.docextract.ingest.domain.MediaType;
import ch.adeon.apps.docextract.structuring.application.StructuringPort;
import ch.adeon.apps.docextract.structuring.domain.StructuredContent;
import ch.adeon.apps.docextract.structuring.domain.StructuringException;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Fast-track alternative to the docling adapter: plain PDFBox text extraction, with a blank line
 * between pages. No heading/table structure is recovered, so the chunker falls back to flat,
 * un-contextualized chunks — useful for local development/tests without the docling container, or
 * as a low-latency degrade path. Selected via {@code
 * docextract.structuring.mode=pdfbox-fast-track}.
 */
@Component
@ConditionalOnProperty(name = "docextract.structuring.mode", havingValue = "pdfbox-fast-track")
public class PdfBoxFastTrackStructuringAdapter implements StructuringPort {

  @Override
  public StructuredContent convert(byte[] content, MediaType mediaType) {
    if (!mediaType.isPdf()) {
      throw new StructuringException(
          "PDFBox fast-track only supports PDF input, got " + mediaType.value());
    }
    try (PDDocument document = Loader.loadPDF(content)) {
      PDFTextStripper stripper = new PDFTextStripper();
      stripper.setParagraphEnd("\n\n");
      String text = stripper.getText(document);
      return new StructuredContent(text);
    } catch (IOException ex) {
      throw new StructuringException("PDFBox text extraction failed", ex);
    }
  }

  @Override
  public boolean supports(MediaType mediaType) {
    return mediaType.isPdf();
  }
}
