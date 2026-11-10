package ch.adeon.apps.docextract.process.application;

import java.util.Optional;

/**
 * Cluster-wide record of where the original was chunk-uploaded in the DMS and under which filename,
 * written at ingest and read by the validation write-back (which may run on another instance than
 * the ingest).
 */
public interface ProcessDocumentPort {

  void save(String tenantId, String processId, String dmsLocation, String filename);

  /** Empty for an unknown process and for one owned by another tenant (NfA-4). */
  Optional<StoredDocument> find(String tenantId, String processId);

  record StoredDocument(String dmsLocation, String filename) {}
}
