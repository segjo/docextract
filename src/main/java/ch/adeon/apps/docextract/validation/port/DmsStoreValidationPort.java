package ch.adeon.apps.docextract.validation.port;

import ch.adeon.apps.docextract.security.application.DvelopCredential;

/**
 * Read-only check of properties against the DMS object definition (ADR-016): nothing is created.
 * Deliberately separate from {@link DmsWritePort} (C-3).
 */
public interface DmsStoreValidationPort {

  /**
   * Validity is a result, not an exception. A DMS that cannot answer (401/403, other 4xx, 5xx,
   * timeout, network) is an {@code AdapterUnavailableException}.
   */
  StoreValidationResult validate(
      String tenantId, StoreValidationRequest request, DvelopCredential credential);
}
