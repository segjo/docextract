package ch.adeon.apps.docextract.extraction.application;

/**
 * Inbound port: read-only lookup of the extraction result last computed for a process, empty if
 * extraction hasn't run yet (mirrors {@code retrieval.application.GetDocumentTypeCandidates}).
 */
public interface GetExtractionResult {

  ExtractionResult get(String tenantId, String processId);
}
