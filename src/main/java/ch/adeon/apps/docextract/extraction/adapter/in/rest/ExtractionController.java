package ch.adeon.apps.docextract.extraction.adapter.in.rest;

import static ch.adeon.apps.docextract.shared.config.OpenApiConfig.BAD_REQUEST_REF;
import static ch.adeon.apps.docextract.shared.config.OpenApiConfig.UNAUTHORIZED_REF;

import ch.adeon.apps.docextract.extraction.application.GetExtractionResult;
import ch.adeon.apps.docextract.extraction.application.RetryExtraction;
import ch.adeon.apps.docextract.extraction.domain.ExtractionResult;
import ch.adeon.apps.docextract.security.application.OutboundCredentialPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /processes/{processId}/extraction-result} — the LLM-suggested document type and
 * writable-property values for this process, for the validation UI (FR-4, SPEC §3). {@code POST
 * /processes/{processId}/extraction-result/retry} — re-runs extraction for this process, forcing
 * the document type to a reviewer-chosen {@code documentTypeId} from the process's staged
 * document-type candidates instead of the LLM's classification ({@link RetryExtraction}).
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "extraction", description = "Attributvorschläge (FR-4)")
public class ExtractionController {

  private final GetExtractionResult getExtractionResult;
  private final RetryExtraction retryExtraction;
  private final OutboundCredentialPort outboundCredentialPort;

  public ExtractionController(
      GetExtractionResult getExtractionResult,
      RetryExtraction retryExtraction,
      OutboundCredentialPort outboundCredentialPort) {
    this.getExtractionResult = getExtractionResult;
    this.retryExtraction = retryExtraction;
    this.outboundCredentialPort = outboundCredentialPort;
  }

  @GetMapping("/processes/{processId}/extraction-result")
  @Operation(
      operationId = "extractionResult",
      summary = "Vorgeschlagene Dokumentart und Attributwerte des Prozesses",
      description = "Leeres Ergebnis, solange keines vorliegt oder der Prozess unbekannt ist.")
  @ApiResponse(responseCode = "200", description = "OK")
  @ApiResponse(responseCode = "401", ref = UNAUTHORIZED_REF)
  public ExtractionResult extractionResult(@PathVariable String processId) {
    return getExtractionResult.get(processId);
  }

  @PostMapping("/processes/{processId}/extraction-result/retry")
  @Operation(
      operationId = "retryExtraction",
      summary = "Extraktion mit vom Menschen gewählter Dokumentart wiederholen")
  @ApiResponse(responseCode = "200", description = "OK")
  @ApiResponse(responseCode = "400", ref = BAD_REQUEST_REF)
  @ApiResponse(responseCode = "401", ref = UNAUTHORIZED_REF)
  public ExtractionResult retry(@PathVariable String processId, @RequestBody RetryRequest request) {
    return retryExtraction.retry(
        processId, request.documentTypeId(), outboundCredentialPort.current());
  }

  public record RetryRequest(String documentTypeId) {}
}
