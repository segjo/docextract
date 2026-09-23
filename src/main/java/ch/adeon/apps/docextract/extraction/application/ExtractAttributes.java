package ch.adeon.apps.docextract.extraction.application;

import ch.adeon.apps.docextract.extraction.domain.ExtractionResult;
import ch.adeon.apps.docextract.security.domain.DvelopCredential;

/**
 * Inbound port for the extraction step (FR-4): classifies the process's staged document type
 * candidates (see {@code retrieval.application.DocumentTypeCandidatesPort}) and suggests values for
 * the winning type's writable properties, grounded in the process's structured document content.
 */
public interface ExtractAttributes {

  ExtractionResult extract(String processId, DvelopCredential credential);
}
