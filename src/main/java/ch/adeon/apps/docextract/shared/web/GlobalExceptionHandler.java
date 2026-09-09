package ch.adeon.apps.docextract.shared.web;

import ch.adeon.apps.docextract.validation.application.ConsentRequiredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ConsentRequiredException.class)
  public ProblemDetail handleConsentRequired(ConsentRequiredException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    problem.setTitle("Consent required");
    return problem;
  }
}
