package ch.adeon.apps.docextract.retrieval.port;

import ch.adeon.apps.docextract.security.domain.DvelopCredential;

/**
 * Outbound port: live lookup of a DMS object's properties by {@code repositoryId}/{@code
 * documentId} (FR-3, per user session, no local cache — SPEC §3). Returns a simplified,
 * indexing-relevant JSON representation ({@link
 * ch.adeon.apps.docextract.retrieval.domain.DmsDocumentMetadata}) — not the raw d.velop response —
 * serialized to a string so callers/storage don't need a dependency on the JSON mapper.
 */
public interface DmsObjectMetadataPort {

  String fetchProperties(String repositoryId, String documentId, DvelopCredential credential);
}
