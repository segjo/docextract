package ch.adeon.apps.docextract.shared.web;

import ch.adeon.apps.docextract.ingest.application.BlobNotFoundException;
import ch.adeon.apps.docextract.ingest.application.DmsChunkUploadException;
import ch.adeon.apps.docextract.ingest.application.PreviewRenderingException;
import ch.adeon.apps.docextract.ingest.application.UploadRejectedException;
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

  @ExceptionHandler(UploadRejectedException.class)
  public ProblemDetail handleUploadRejected(UploadRejectedException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problem.setTitle("Upload rejected");
    return problem;
  }

  // Same status for "no such blob" and "not your blob" (NfA-4): never confirm cross-tenant/-user
  // existence.
  @ExceptionHandler(BlobNotFoundException.class)
  public ProblemDetail handleBlobNotFound(BlobNotFoundException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setTitle("Blob not found");
    return problem;
  }

  @ExceptionHandler({DmsChunkUploadException.class, PreviewRenderingException.class})
  public ProblemDetail handleUpstreamFailure(RuntimeException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
    problem.setTitle("Upstream service failure");
    return problem;
  }
}
