package ch.adeon.apps.docextract.ingest.application;

import ch.adeon.apps.docextract.ingest.domain.BlobKind;
import ch.adeon.apps.docextract.ingest.domain.BlobRef;
import ch.adeon.apps.docextract.ingest.domain.MediaType;
import ch.adeon.apps.docextract.ingest.domain.PageRange;
import java.util.UUID;

/**
 * Outbound port for the transient Postgres blobstore ({@code DOCUMENT_BLOB} / {@code
 * DOCUMENT_BLOB_PAGE}), the cluster-visible handover point for the async job and the Range-fähige
 * source for the preview stream (ADR-008). Replaces the DMS-Chunk-Store as a read-back source,
 * since the DMS chunk upload is write-only.
 */
public interface DocumentBlobPort {

  BlobRef store(
      BlobKind kind,
      MediaType mediaType,
      byte[] content,
      String processId,
      String tenantId,
      String userId);

  BlobRef describe(UUID blobId);

  byte[] readRange(UUID blobId, PageRange range);

  void delete(UUID blobId);
}
