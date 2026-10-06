package ch.adeon.apps.docextract.extraction.adapter.in.rest;

import ch.adeon.apps.docextract.extraction.application.GetExtractionResult;
import ch.adeon.apps.docextract.extraction.domain.ExtractionResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /processes/{processId}/extraction-result} — the LLM-suggested document type and
 * writable-property values for this process, for the validation UI (FR-4, SPEC §3).
 */
@RestController
@RequestMapping("/api/v1")
public class ExtractionController {

  private final GetExtractionResult getExtractionResult;

  public ExtractionController(GetExtractionResult getExtractionResult) {
    this.getExtractionResult = getExtractionResult;
  }

  @GetMapping("/processes/{processId}/extraction-result")
  public ExtractionResult extractionResult(@PathVariable String processId) {
    return getExtractionResult.get(processId);
  }
}
