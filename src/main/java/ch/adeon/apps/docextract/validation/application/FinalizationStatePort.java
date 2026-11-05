package ch.adeon.apps.docextract.validation.application;

import ch.adeon.apps.docextract.validation.domain.Finalization;
import ch.adeon.apps.docextract.validation.domain.FinalizationStatus;
import java.util.List;
import java.util.Optional;

/**
 * Cluster-wide saga state per process (ADR-015): written right after the DMS write so that any
 * instance can resume with promotion only.
 */
public interface FinalizationStatePort {

  void save(Finalization finalization);

  Optional<Finalization> find(String processId);

  List<Finalization> findByStatus(FinalizationStatus status);

  /** Removes a {@code DMS_WRITE_STARTED} marker after a clean DMS rejection; other states stay. */
  void discardWriteStarted(String processId);

  /**
   * No TTL: rows are only removed on an explicit deletion request per tenant (NfA-5). TODO: no
   * deletion-request flow calls this yet.
   */
  void deleteByTenant(String tenantId);
}
