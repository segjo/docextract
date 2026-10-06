package ch.adeon.apps.docextract.content.application;

import ch.adeon.apps.docextract.content.domain.ContentCommand;
import ch.adeon.apps.docextract.content.domain.ContentException;
import ch.adeon.apps.docextract.content.domain.ExtractedText;
import ch.adeon.apps.docextract.content.port.TextExtractionPort;
import ch.adeon.apps.docextract.ingest.port.DocumentBlobPort;
import ch.adeon.apps.docextract.ingest.domain.BlobRef;
import ch.adeon.apps.docextract.ingest.domain.MediaType;
import ch.adeon.apps.docextract.ingest.domain.PageRange;
import ch.adeon.apps.docextract.process.application.ProcessEventPort;
import ch.adeon.apps.docextract.process.domain.ProcessEvent;
import ch.adeon.apps.docextract.process.domain.ProcessStep;
import ch.adeon.apps.docextract.process.domain.StepStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Reads the ORIGINAL blob (ADR-008) and extracts the first-N-words plain text via the configured
 * {@link TextExtractionPort} (ADR-012: no chunking, no structure/table reconstruction) for FR-3.
 * The result is staged in-memory (ADR-006) for retrieval to consume. Progress is reported via
 * {@link ProcessEventPort} so SSE clients see {@code text_extracted}.
 */
@Service
public class ExtractTextService implements ExtractText {

  private static final Logger log = LoggerFactory.getLogger(ExtractTextService.class);

  private final DocumentBlobPort documentBlobPort;
  private final TextExtractionPort textExtractionPort;
  private final TextStagingPort textStagingPort;
  private final ProcessEventPort processEventPort;
  private final int maxWords;

  public ExtractTextService(
      DocumentBlobPort documentBlobPort,
      TextExtractionPort textExtractionPort,
      TextStagingPort textStagingPort,
      ProcessEventPort processEventPort,
      @Value("${docextract.content.max-words:5000}") int maxWords) {
    this.documentBlobPort = documentBlobPort;
    this.textExtractionPort = textExtractionPort;
    this.textStagingPort = textStagingPort;
    this.processEventPort = processEventPort;
    this.maxWords = maxWords;
  }

  @Override
  public ExtractedText extractText(ContentCommand command) {
    log.info(
        "text extraction started processId={} blobId={}", command.processId(), command.blobId());
    processEventPort.publish(
        new ProcessEvent(
            command.processId(), ProcessStep.TEXT_EXTRACTED, StepStatus.STARTED, null));
    try {
      byte[] content = readOriginal(command.blobId());
      ExtractedText extractedText =
          textExtractionPort.extract(content, command.mediaType(), maxWords);
      textStagingPort.stage(command.processId(), extractedText);
      log.info(
          "text extraction completed processId={} wordCount={}",
          command.processId(),
          extractedText.wordCount());
      processEventPort.publish(
          new ProcessEvent(
              command.processId(), ProcessStep.TEXT_EXTRACTED, StepStatus.COMPLETED, null));
      return extractedText;
    } catch (RuntimeException ex) {
      log.warn("text extraction failed processId={}", command.processId(), ex);
      processEventPort.publish(
          new ProcessEvent(
              command.processId(), ProcessStep.TEXT_EXTRACTED, StepStatus.FAILED, null));
      throw new ContentException("Text extraction failed for process " + command.processId(), ex);
    }
  }

  private byte[] readOriginal(java.util.UUID blobId) {
    BlobRef blob = documentBlobPort.describe(blobId);
    return documentBlobPort.readRange(blobId, PageRange.full(blob.sizeBytes()));
  }

  @Override
  public boolean supports(MediaType mediaType) {
    return textExtractionPort.supports(mediaType);
  }
}
