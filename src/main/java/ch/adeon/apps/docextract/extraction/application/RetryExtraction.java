package ch.adeon.apps.docextract.extraction.application;

import ch.adeon.apps.docextract.extraction.domain.ExtractionResult;
import ch.adeon.apps.docextract.security.domain.DvelopCredential;

/**
 * Inbound port: re-runs the extraction step (FR-4) for a process, forcing the document type to the
 * given {@code documentTypeId} instead of letting the LLM classify it. Used by the validation UI
 * when the reviewer overrides the LLM's document type suggestion with one of the process's staged
 * {@link DocumentTypeCandidatesPort} candidates. Relies on the document content still being staged
 * in-memory for this process (ADR-006) — only possible within the staging TTL after ingestion.
 */
public interface RetryExtraction {

  ExtractionResult retry(String processId, String documentTypeId, DvelopCredential credential);
}
