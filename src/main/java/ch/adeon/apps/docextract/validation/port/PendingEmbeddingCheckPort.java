package ch.adeon.apps.docextract.validation.port;

import ch.adeon.apps.docextract.validation.domain.PendingEmbeddingCheck;
import java.util.Optional;

/**
 * Read-only lookup of the caller's process's {@code PENDING} embedding (expiry) before the DMS
 * write (ARCHITECTURE § 6.2, NfA-4). Empty if none exists for this tenant, e.g. when retrieval was
 * skipped (§ 6.5).
 */
public interface PendingEmbeddingCheckPort {

  Optional<PendingEmbeddingCheck> find(String tenantId, String processId);
}
