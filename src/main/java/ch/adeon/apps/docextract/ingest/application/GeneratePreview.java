package ch.adeon.apps.docextract.ingest.application;

import ch.adeon.apps.docextract.ingest.domain.MediaType;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Triggers preview generation right after a successful upload, decoupled from the synchronous
 * {@code 202 Accepted} response (ADR-008). Progress is reported via {@code process.application.
 * ProcessEventPort} so the client can fetch the resulting blob from {@code PreviewController} once
 * ready. The returned future completes with the separately stored PREVIEW blob id, or empty when no
 * extra blob was created (native PDF, or rendering failed) — callers use it to roll back an
 * orphaned preview blob if the surrounding ingest fails.
 */
public interface GeneratePreview {

  CompletableFuture<Optional<UUID>> generate(
      String processId, UUID originalBlobId, MediaType mediaType, String tenantId, String userId);
}
