package ch.adeon.apps.docextract.content.application;

import ch.adeon.apps.docextract.shared.media.MediaType;
import java.util.UUID;

/**
 * Requests text/content extraction for a document already held in the ingest blobstore (ADR-008).
 * {@code blobId} points at the ORIGINAL blob.
 */
public record ContentCommand(
    String processId, UUID blobId, MediaType mediaType, String tenantId, String userId) {}
