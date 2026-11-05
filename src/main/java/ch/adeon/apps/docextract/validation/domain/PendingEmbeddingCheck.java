package ch.adeon.apps.docextract.validation.domain;

/**
 * Owner and validity of a process's quarantined embedding, checked before the DMS write (§ 6.2).
 */
public record PendingEmbeddingCheck(String tenantId, boolean expired) {}
