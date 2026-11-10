package ch.adeon.apps.docextract.extraction.application;

import ch.adeon.apps.docextract.process.application.ProcessEventPort;
import ch.adeon.apps.docextract.process.application.ProcessNotFoundException;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class GetExtractionResultService implements GetExtractionResult {

  private final ExtractionResultPort extractionResultPort;
  private final ProcessEventPort processEventPort;

  public GetExtractionResultService(
      ExtractionResultPort extractionResultPort, ProcessEventPort processEventPort) {
    this.extractionResultPort = extractionResultPort;
    this.processEventPort = processEventPort;
  }

  /** Empty result: the process exists but has none yet, otherwise it is unknown (404). */
  @Override
  public ExtractionResult get(String tenantId, String processId) {
    Optional<ExtractionResult> found = extractionResultPort.find(tenantId, processId);
    if (found.isPresent()) {
      return found.get();
    }
    if (!processEventPort.exists(tenantId, processId)) {
      throw new ProcessNotFoundException(processId);
    }
    return ExtractionResult.EMPTY;
  }
}
