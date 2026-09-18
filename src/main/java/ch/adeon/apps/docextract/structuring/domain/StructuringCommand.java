package ch.adeon.apps.docextract.structuring.domain;

import ch.adeon.apps.docextract.ingest.domain.MediaType;
import java.util.UUID;

/**
 * Requests structuring of a document already held in the ingest blobstore (ADR-008). {@code blobId}
 * points at the ORIGINAL blob — structuring reads the source bytes, not the human-facing preview.
 */
public record StructuringCommand(
    String processId, UUID blobId, MediaType mediaType, String tenantId, String userId) {}
