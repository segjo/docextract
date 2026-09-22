package ch.adeon.apps.docextract.retrieval.domain;

/**
 * One of the top-K most similar already-{@code APPROVED} documents (FR-3, NfA-6). {@code
 * documentId} is the d.velop DMS object id, attached to the embedding only at promotion time —
 * so it is always present here, since PENDING rows are never returned (ADR-002/-006). {@code
 * properties} is the raw DMS object-properties response (FR-3, SPEC §3), fetched live per result;
 * {@code null} if {@code repositoryId}/{@code documentId} was blank or the DMS call failed.
 */
public record RetrievalResult(
    String repositoryId, String documentId, double score, String properties) {}
