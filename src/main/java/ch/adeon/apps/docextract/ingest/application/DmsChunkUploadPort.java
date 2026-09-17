package ch.adeon.apps.docextract.ingest.application;

import ch.adeon.apps.docextract.ingest.domain.DmsLocation;
import ch.adeon.apps.docextract.ingest.domain.MediaType;
import ch.adeon.apps.docextract.security.domain.DvelopCredential;

/**
 * Outbound port for the write-only d.velop DMS-Chunk-Store (ADR-008). Used solely to obtain the
 * {@code Location} that later serves as the finalization target after consent; the uploaded bytes
 * cannot be read back from the DMS before that.
 */
public interface DmsChunkUploadPort {

  // Credential is passed explicitly (not read from SecurityContextHolder) because this runs on an
  // executor thread that doesn't inherit the request thread's ThreadLocal security context.
  DmsLocation upload(byte[] content, MediaType mediaType, DvelopCredential credential);
}
