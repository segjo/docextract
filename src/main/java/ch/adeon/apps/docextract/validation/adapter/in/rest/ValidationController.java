package ch.adeon.apps.docextract.validation.adapter.in.rest;

import ch.adeon.apps.docextract.validation.application.ConfirmAndWriteBack;
import ch.adeon.apps.docextract.validation.application.RejectSuggestion;
import ch.adeon.apps.docextract.validation.domain.ConfirmationCommand;
import ch.adeon.apps.docextract.validation.domain.ConfirmationResult;
import ch.adeon.apps.docextract.validation.domain.RejectionCommand;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/validation")
public class ValidationController {

  private final ConfirmAndWriteBack confirmAndWriteBack;
  private final RejectSuggestion rejectSuggestion;

  public ValidationController(
      ConfirmAndWriteBack confirmAndWriteBack, RejectSuggestion rejectSuggestion) {
    this.confirmAndWriteBack = confirmAndWriteBack;
    this.rejectSuggestion = rejectSuggestion;
  }

  @PostMapping("/confirm")
  public ResponseEntity<ConfirmationResult> confirm(@RequestBody ConfirmRequest request) {
    ConfirmationResult result =
        confirmAndWriteBack.confirm(
            new ConfirmationCommand(
                request.processId(),
                request.documentId(),
                request.repositoryId(),
                request.consentGiven(),
                request.templateConsent(),
                request.attributes()));
    return ResponseEntity.ok(result);
  }

  @PostMapping("/reject")
  public ResponseEntity<Void> reject(@RequestBody RejectRequest request) {
    rejectSuggestion.reject(new RejectionCommand(request.processId()));
    return ResponseEntity.noContent().build();
  }

  public record ConfirmRequest(
      String processId,
      String documentId,
      String repositoryId,
      boolean consentGiven,
      boolean templateConsent,
      Map<String, Object> attributes) {}

  public record RejectRequest(String processId) {}
}
