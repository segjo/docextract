package ch.adeon.apps.docextract.retrieval.application;

import ch.adeon.apps.docextract.retrieval.domain.DmsDocumentMetadata;
import java.util.List;
import java.util.Optional;

/**
 * TTL-bounded handover of a process's document type candidates to the next processing step /
 * validation UI (SPEC §3), analogous to {@link RetrievalResultPort} for the similar-documents case:
 * {@link FindSimilarService} stores either the best-matched type's {@link DmsDocumentMetadata}
 * (read back from an already-fetched hit, no extra DMS call) or, when no similar document was
 * found, all repository document types fetched as a fallback. Must be readable from any cluster
 * node, not just the one that ran the fetch (ADR-004).
 */
public interface DocumentTypeCandidatesPort {

  void store(String processId, List<DmsDocumentMetadata> candidates);

  Optional<List<DmsDocumentMetadata>> find(String processId);
}
