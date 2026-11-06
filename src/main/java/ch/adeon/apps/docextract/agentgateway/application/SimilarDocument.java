package ch.adeon.apps.docextract.agentgateway.application;

/**
 * One of the top-K {@code APPROVED} documents similar to a process (FR-3), without DMS metadata.
 */
public record SimilarDocument(String documentId, String repositoryId, double score) {}
