package ch.adeon.apps.docextract.retrieval.application;

import ch.adeon.apps.docextract.retrieval.domain.DmsDocumentMetadata;
import ch.adeon.apps.docextract.security.domain.DvelopCredential;
import java.util.List;

/**
 * Outbound port: reads all possible document type categories and their properties (incl. data type)
 * for a repository via {@code GET /r/{repositoryId}/objdef} (SPEC §3), reduced to the same {@link
 * DmsDocumentMetadata} shape as a single hit's live properties (no {@code contentLanguage},
 * property values, or per-property {@code definition} flags — objdef doesn't carry those). Used as
 * the fallback candidate set for extraction when no similar {@code APPROVED} document narrowed down
 * the document type.
 */
public interface DmsObjectDefinitionPort {

  List<DmsDocumentMetadata> fetchAll(String repositoryId, DvelopCredential credential);
}
