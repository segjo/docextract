package ch.adeon.apps.docextract.validation.domain;

/** Validity of a process's quarantined embedding, checked before the DMS write (§ 6.2). */
public record PendingEmbeddingCheck(boolean expired) {}
