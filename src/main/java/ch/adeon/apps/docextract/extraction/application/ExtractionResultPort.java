package ch.adeon.apps.docextract.extraction.application;

import ch.adeon.apps.docextract.extraction.domain.ExtractionResult;
import java.util.Optional;

/**
 * TTL-bounded handover of a process's extraction result to the validation UI/MCP (SPEC §3), same
 * cluster-mode reasoning as {@code retrieval.application.RetrievalResultPort}/{@code
 * DocumentTypeCandidatesPort}: must be readable from any node, not just the one that ran extraction
 * (ADR-004).
 */
public interface ExtractionResultPort {

  void store(String processId, ExtractionResult result);

  Optional<ExtractionResult> find(String processId);
}
