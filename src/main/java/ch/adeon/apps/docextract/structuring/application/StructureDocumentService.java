package ch.adeon.apps.docextract.structuring.application;

import ch.adeon.apps.docextract.ingest.application.DocumentBlobPort;
import ch.adeon.apps.docextract.ingest.domain.BlobRef;
import ch.adeon.apps.docextract.ingest.domain.MediaType;
import ch.adeon.apps.docextract.ingest.domain.PageRange;
import ch.adeon.apps.docextract.process.application.ProcessEventPort;
import ch.adeon.apps.docextract.process.domain.ProcessEvent;
import ch.adeon.apps.docextract.process.domain.ProcessStep;
import ch.adeon.apps.docextract.process.domain.StepStatus;
import ch.adeon.apps.docextract.structuring.domain.ChunkingConfig;
import ch.adeon.apps.docextract.structuring.domain.DocChunk;
import ch.adeon.apps.docextract.structuring.domain.HeadingAwareChunker;
import ch.adeon.apps.docextract.structuring.domain.StructuredContent;
import ch.adeon.apps.docextract.structuring.domain.StructuredDocument;
import ch.adeon.apps.docextract.structuring.domain.StructuringCommand;
import ch.adeon.apps.docextract.structuring.domain.StructuringException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Reads the ORIGINAL blob (ADR-008), converts it via the configured {@link StructuringPort}
 * (docling by default, ADR-007), splits the result into contextualized chunks and stages them
 * in-memory (ADR-006) for the retrieval/extraction steps. Progress is reported via {@link
 * ProcessEventPort} so SSE clients see {@code structured}.
 */
@Service
public class StructureDocumentService implements StructureDocument {

  private static final Logger log = LoggerFactory.getLogger(StructureDocumentService.class);

  private final DocumentBlobPort documentBlobPort;
  private final StructuringPort structuringPort;
  private final ChunkStagingPort chunkStagingPort;
  private final ProcessEventPort processEventPort;
  private final int maxChunkTokens;

  public StructureDocumentService(
      DocumentBlobPort documentBlobPort,
      StructuringPort structuringPort,
      ChunkStagingPort chunkStagingPort,
      ProcessEventPort processEventPort,
      @Value("${docextract.structuring.max-chunk-tokens:512}") int maxChunkTokens) {
    this.documentBlobPort = documentBlobPort;
    this.structuringPort = structuringPort;
    this.chunkStagingPort = chunkStagingPort;
    this.processEventPort = processEventPort;
    this.maxChunkTokens = maxChunkTokens;
  }

  @Override
  public StructuredDocument structure(StructuringCommand command) {
    log.info("structuring started processId={} blobId={}", command.processId(), command.blobId());
    processEventPort.publish(
        new ProcessEvent(command.processId(), ProcessStep.STRUCTURED, StepStatus.STARTED, null));
    try {
      BlobRef blob = documentBlobPort.describe(command.blobId());
      byte[] content =
          documentBlobPort.readRange(command.blobId(), PageRange.full(blob.sizeBytes()));

      StructuredContent structuredContent = structuringPort.convert(content, blob.mediaType());
      List<DocChunk> chunks =
          HeadingAwareChunker.chunk(structuredContent, new ChunkingConfig(maxChunkTokens));

      chunkStagingPort.stage(command.processId(), chunks);
      log.info(
          "structuring completed processId={} chunkCount={}", command.processId(), chunks.size());
      processEventPort.publish(
          new ProcessEvent(
              command.processId(), ProcessStep.STRUCTURED, StepStatus.COMPLETED, null));
      return new StructuredDocument(command.processId(), chunks);
    } catch (RuntimeException ex) {
      log.warn("structuring failed processId={}", command.processId(), ex);
      processEventPort.publish(
          new ProcessEvent(command.processId(), ProcessStep.STRUCTURED, StepStatus.FAILED, null));
      throw new StructuringException("Structuring failed for process " + command.processId(), ex);
    }
  }

  @Override
  public boolean supports(MediaType mediaType) {
    return structuringPort.supports(mediaType);
  }
}
