package ch.adeon.apps.docextract.retrieval.application;

import ch.adeon.apps.docextract.retrieval.domain.RetrievalResult;
import java.util.List;

/**
 * Inbound port: read-only lookup of the similar documents last found for a process, for display
 * next to the validation UI (FR-3).
 */
public interface GetSimilarDocuments {

  List<RetrievalResult> get(String processId);
}
