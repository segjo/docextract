package ch.adeon.apps.docextract.retrieval.application;

import ch.adeon.apps.docextract.process.application.ProcessEventPort;
import ch.adeon.apps.docextract.process.application.ProcessNotFoundException;
import ch.adeon.apps.docextract.retrieval.port.DmsDocumentMetadata;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class GetDocumentTypeCandidatesService implements GetDocumentTypeCandidates {

  private final DocumentTypeCandidatesPort documentTypeCandidatesPort;
  private final ProcessEventPort processEventPort;

  public GetDocumentTypeCandidatesService(
      DocumentTypeCandidatesPort documentTypeCandidatesPort, ProcessEventPort processEventPort) {
    this.documentTypeCandidatesPort = documentTypeCandidatesPort;
    this.processEventPort = processEventPort;
  }

  /** Empty result: the process exists but has none yet, otherwise it is unknown (404). */
  @Override
  public List<DmsDocumentMetadata> get(String tenantId, String processId) {
    Optional<List<DmsDocumentMetadata>> found =
        documentTypeCandidatesPort.find(tenantId, processId);
    if (found.isPresent()) {
      return found.get();
    }
    if (!processEventPort.exists(tenantId, processId)) {
      throw new ProcessNotFoundException(processId);
    }
    return List.of();
  }
}
