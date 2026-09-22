package ch.adeon.apps.docextract.retrieval.application;

import ch.adeon.apps.docextract.retrieval.domain.DmsDocumentMetadata;
import java.util.List;

/**
 * Inbound port: read-only lookup of the document type candidates last determined for a process
 * (SPEC §3) — either the best-matched type from a similarity search, or the full repository
 * fallback set when no similar document was found, empty if retrieval hasn't run yet.
 */
public interface GetDocumentTypeCandidates {

  List<DmsDocumentMetadata> get(String processId);
}
