package ch.adeon.apps.docextract.retrieval.application;

import ch.adeon.apps.docextract.retrieval.domain.DmsDocumentMetadata;
import java.util.List;

/**
 * Inbound port: live lookup of every document type + property definition in the repository (SPEC
 * §3, no document instance/values, only the schema) — the same data {@link FindSimilarService}
 * falls back to as document type candidates when no similar document was found, exposed directly
 * so the frontend can offer the full catalog independently of a running process.
 */
public interface GetObjectDefinitions {

  List<DmsDocumentMetadata> get();
}
