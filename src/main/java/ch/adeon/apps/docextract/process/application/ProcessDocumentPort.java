package ch.adeon.apps.docextract.process.application;

import java.util.Optional;

/**
 * Cluster-wide record of where the original was chunk-uploaded in the DMS and under which filename,
 * written at ingest and read by the validation write-back (which may run on another instance than
 * the ingest).
 */
public interface ProcessDocumentPort {

  void save(String processId, String dmsLocation, String filename);

  Optional<StoredDocument> find(String processId);

  /** {@code tenantId} is the process owner, used for the tenant check on confirm (NfA-4). */
  record StoredDocument(String dmsLocation, String filename, String tenantId) {}
}
