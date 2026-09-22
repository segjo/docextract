package ch.adeon.apps.docextract.retrieval.application;

import ch.adeon.apps.docextract.retrieval.domain.RetrievalResult;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetSimilarDocumentsService implements GetSimilarDocuments {

  private final RetrievalResultPort retrievalResultPort;

  public GetSimilarDocumentsService(RetrievalResultPort retrievalResultPort) {
    this.retrievalResultPort = retrievalResultPort;
  }

  @Override
  public List<RetrievalResult> get(String processId) {
    return retrievalResultPort.find(processId).orElse(List.of());
  }
}
