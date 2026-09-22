package ch.adeon.apps.docextract.retrieval.adapter.in.rest;

import ch.adeon.apps.docextract.retrieval.application.GetDocumentTypeCandidates;
import ch.adeon.apps.docextract.retrieval.application.GetObjectDefinitions;
import ch.adeon.apps.docextract.retrieval.application.GetSimilarDocuments;
import ch.adeon.apps.docextract.retrieval.application.GetValueList;
import ch.adeon.apps.docextract.retrieval.domain.DmsDocumentMetadata;
import ch.adeon.apps.docextract.retrieval.domain.DmsValueList;
import ch.adeon.apps.docextract.retrieval.domain.RetrievalResult;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /processes/{processId}/similar-documents} — the already-{@code APPROVED} documents
 * found similar to this process, for display next to the validation UI (FR-3, NfA-6). {@code GET
 * /processes/{processId}/document-type-candidates} — the document type candidate(s) for this
 * process (SPEC §3): the best-matched type if similar documents were found, otherwise the full
 * repository fallback set. {@code GET /object-definitions} — the full repository catalog of
 * document types/properties, live, independent of any process (SPEC §3). {@code POST
 * /value-lists/{propertyId}} — a single property's valid values, live, for frontend typeahead/"load
 * more" filtering via {@code searchTerm} (SPEC §3, {@link
 * ch.adeon.apps.docextract.retrieval.application.ValueListPort}).
 */
@RestController
@RequestMapping("/api/v1")
public class RetrievalController {

  private final GetSimilarDocuments getSimilarDocuments;
  private final GetDocumentTypeCandidates getDocumentTypeCandidates;
  private final GetObjectDefinitions getObjectDefinitions;
  private final GetValueList getValueList;

  public RetrievalController(
      GetSimilarDocuments getSimilarDocuments,
      GetDocumentTypeCandidates getDocumentTypeCandidates,
      GetObjectDefinitions getObjectDefinitions,
      GetValueList getValueList) {
    this.getSimilarDocuments = getSimilarDocuments;
    this.getDocumentTypeCandidates = getDocumentTypeCandidates;
    this.getObjectDefinitions = getObjectDefinitions;
    this.getValueList = getValueList;
  }

  @GetMapping("/processes/{processId}/similar-documents")
  public List<RetrievalResult> similarDocuments(@PathVariable String processId) {
    return getSimilarDocuments.get(processId);
  }

  @GetMapping("/processes/{processId}/document-type-candidates")
  public List<DmsDocumentMetadata> documentTypeCandidates(@PathVariable String processId) {
    return getDocumentTypeCandidates.get(processId);
  }

  @GetMapping("/object-definitions")
  public List<DmsDocumentMetadata> objectDefinitions() {
    return getObjectDefinitions.get();
  }

  @PostMapping("/value-lists/{propertyId}")
  public DmsValueList valueList(
      @PathVariable String propertyId, @RequestBody ValueListRequest request) {
    return getValueList.get(
        request.objectDefinitionId(),
        propertyId,
        request.extendedProperties(),
        request.multivalueExtendedProperties(),
        request.searchTerm());
  }

  public record ValueListRequest(
      String objectDefinitionId,
      Map<String, String> extendedProperties,
      Map<String, List<String>> multivalueExtendedProperties,
      String searchTerm) {}
}
