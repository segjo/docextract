package ch.adeon.apps.docextract.retrieval.application;

import ch.adeon.apps.docextract.retrieval.domain.DmsDocumentMetadata;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetDocumentTypeCandidatesService implements GetDocumentTypeCandidates {

  private final DocumentTypeCandidatesPort documentTypeCandidatesPort;

  public GetDocumentTypeCandidatesService(DocumentTypeCandidatesPort documentTypeCandidatesPort) {
    this.documentTypeCandidatesPort = documentTypeCandidatesPort;
  }

  @Override
  public List<DmsDocumentMetadata> get(String processId) {
    return documentTypeCandidatesPort.find(processId).orElse(List.of());
  }
}
