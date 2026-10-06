package ch.adeon.apps.docextract.content.application;

import ch.adeon.apps.docextract.content.domain.ContentCommand;
import ch.adeon.apps.docextract.content.domain.ContentException;
import ch.adeon.apps.docextract.content.domain.DocumentContent;
import ch.adeon.apps.docextract.content.port.DocumentContentPort;
import ch.adeon.apps.docextract.ingest.domain.BlobRef;
import ch.adeon.apps.docextract.ingest.domain.MediaType;
import ch.adeon.apps.docextract.ingest.domain.PageRange;
import ch.adeon.apps.docextract.ingest.port.DocumentBlobPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Reads the ORIGINAL blob (ADR-008) and builds the document representation used for FR-4, via the
 * configured {@link DocumentContentPort}. Staged in-memory (ADR-006) for extraction to consume,
 * independent of {@link ExtractTextService}'s first-N-words text for FR-3 (SPEC §2).
 */
@Service
public class ContentService implements ProvideContent {

  private static final Logger log = LoggerFactory.getLogger(ContentService.class);

  private final DocumentBlobPort documentBlobPort;
  private final DocumentContentPort documentContentPort;
  private final ContentStagingPort contentStagingPort;

  public ContentService(
      DocumentBlobPort documentBlobPort,
      DocumentContentPort documentContentPort,
      ContentStagingPort contentStagingPort) {
    this.documentBlobPort = documentBlobPort;
    this.documentContentPort = documentContentPort;
    this.contentStagingPort = contentStagingPort;
  }

  @Override
  public DocumentContent provide(ContentCommand command) {
    try {
      BlobRef blob = documentBlobPort.describe(command.blobId());
      byte[] bytes = documentBlobPort.readRange(command.blobId(), PageRange.full(blob.sizeBytes()));
      DocumentContent content = documentContentPort.provide(bytes, command.mediaType());
      contentStagingPort.stage(command.processId(), content);
      return content;
    } catch (RuntimeException ex) {
      log.warn("content extraction failed processId={}", command.processId(), ex);
      throw new ContentException(
          "Content extraction failed for process " + command.processId(), ex);
    }
  }

  @Override
  public boolean supports(MediaType mediaType) {
    return documentContentPort.supports(mediaType);
  }
}
