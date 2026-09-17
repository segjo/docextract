package ch.adeon.apps.docextract.process.domain;

/**
 * PII-free progress notification pushed to SSE subscribers of a {@code processId} (ADR-004, C-4).
 * {@code blobId} is only set once a blob (e.g. the rendered preview) is available for the client to
 * fetch via the existing blob-read endpoints.
 */
public record ProcessEvent(String processId, ProcessStep step, StepStatus status, String blobId) {}
