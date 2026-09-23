package ch.adeon.apps.docextract.extraction.application;

import ch.adeon.apps.docextract.extraction.domain.ExtractionResult;
import org.springframework.stereotype.Service;

@Service
public class GetExtractionResultService implements GetExtractionResult {

  private final ExtractionResultPort extractionResultPort;

  public GetExtractionResultService(ExtractionResultPort extractionResultPort) {
    this.extractionResultPort = extractionResultPort;
  }

  @Override
  public ExtractionResult get(String processId) {
    return extractionResultPort.find(processId).orElse(ExtractionResult.EMPTY);
  }
}
