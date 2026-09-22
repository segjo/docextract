package ch.adeon.apps.docextract.retrieval.application;

import ch.adeon.apps.docextract.retrieval.domain.RetrievalResult;
import java.util.List;
import java.util.Optional;

/**
 * TTL-bounded handover of a process's last similarity search result to the UI (FR-3): {@link
 * FindSimilarService} stores the {@code APPROVED}/min-score-filtered hits it just computed, and
 * {@link GetSimilarDocumentsService} reads them back for the validation screen. Must be readable
 * from any cluster node, not just the one that ran the search (ADR-004) — implementations persist
 * to Postgres, not per-instance memory. Only document ids/scores, no raw content (ADR-006).
 */
public interface RetrievalResultPort {

  void store(String processId, List<RetrievalResult> results);

  Optional<List<RetrievalResult>> find(String processId);
}
