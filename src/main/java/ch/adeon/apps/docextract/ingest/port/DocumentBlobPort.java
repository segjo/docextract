package ch.adeon.apps.docextract.ingest.port;

import ch.adeon.apps.docextract.ingest.domain.BlobKind;
import ch.adeon.apps.docextract.shared.media.MediaType;
import java.util.List;
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

  /** Deletes all blobs (ORIGINAL, PREVIEW) of a process, e.g. after finalization (§ 6.2). */
  void deleteByProcess(String processId);

  /** Process ids with at least one blob past {@code expires_at}. */
  List<String> findExpiredProcessIds();

  void deleteExpired(String processId);
}
