package ch.adeon.apps.docextract.retrieval.application;

import ch.adeon.apps.docextract.process.application.ProcessEventPort;
import ch.adeon.apps.docextract.process.application.ProcessNotFoundException;
import ch.adeon.apps.docextract.retrieval.port.RetrievalResult;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class GetSimilarDocumentsService implements GetSimilarDocuments {

  private final RetrievalResultPort retrievalResultPort;
  private final ProcessEventPort processEventPort;

  public GetSimilarDocumentsService(
      RetrievalResultPort retrievalResultPort, ProcessEventPort processEventPort) {
    this.retrievalResultPort = retrievalResultPort;
    this.processEventPort = processEventPort;
  }

  /** Empty result: the process exists but has none yet, otherwise it is unknown (404). */
  @Override
  public List<RetrievalResult> get(String tenantId, String processId) {
    Optional<List<RetrievalResult>> found = retrievalResultPort.find(tenantId, processId);
    if (found.isPresent()) {
      return found.get();
    }
    if (!processEventPort.exists(tenantId, processId)) {
      throw new ProcessNotFoundException(processId);
    }
    return List.of();
  }
}
