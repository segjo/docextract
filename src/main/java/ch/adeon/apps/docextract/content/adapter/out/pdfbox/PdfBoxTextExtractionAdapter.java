package ch.adeon.apps.docextract.content.adapter.out.pdfbox;

import ch.adeon.apps.docextract.content.domain.ContentException;
import ch.adeon.apps.docextract.content.domain.ExtractedText;
import ch.adeon.apps.docextract.content.port.TextExtractionPort;
import ch.adeon.apps.docextract.ingest.domain.MediaType;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Reference {@link TextExtractionPort} adapter (ADR-011/-012, local): plain PDFBox text extraction,
 * truncated to the first {@code maxWords} words — no structure/table reconstruction. An empty or
 * too-short result is a valid outcome (E-5 "unbekannt" beats a spurious failure), not an error.
 */
@Component
@ConditionalOnProperty(
    name = "docextract.adapters.text-extraction",
    havingValue = "pdfbox",
    matchIfMissing = true)
public class PdfBoxTextExtractionAdapter implements TextExtractionPort {

  @Override
  public ExtractedText extract(byte[] content, MediaType mediaType, int maxWords) {
    if (!mediaType.isPdf()) {
      throw new ContentException(
          "PDFBox text extraction only supports PDF input, got " + mediaType.value());
    }
    try (PDDocument document = Loader.loadPDF(content)) {
      String text = new PDFTextStripper().getText(document);
      return truncate(text, maxWords);
    } catch (IOException ex) {
      throw new ContentException("PDFBox text extraction failed", ex);
    }
  }

  private static ExtractedText truncate(String text, int maxWords) {
    String trimmed = text == null ? "" : text.strip();
    if (trimmed.isEmpty()) {
      return ExtractedText.empty();
    }
    String[] words = trimmed.split("\\s+");
    int wordCount = Math.min(words.length, maxWords);
    String truncated = String.join(" ", java.util.Arrays.copyOfRange(words, 0, wordCount));
    return new ExtractedText(truncated, wordCount);
  }

  @Override
  public boolean supports(MediaType mediaType) {
    return mediaType.isPdf();
  }
}
