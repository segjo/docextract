package ch.adeon.apps.docextract.shared.web;

import ch.adeon.apps.docextract.ingest.application.BlobNotFoundException;
import ch.adeon.apps.docextract.ingest.application.DmsChunkUploadException;
import ch.adeon.apps.docextract.ingest.application.PreviewRenderingException;
import ch.adeon.apps.docextract.ingest.application.UploadRejectedException;
import ch.adeon.apps.docextract.process.application.ProcessNotFoundException;
import ch.adeon.apps.docextract.validation.application.AlreadyFinalizedException;
import ch.adeon.apps.docextract.validation.application.InvalidAttributesException;
import ch.adeon.apps.docextract.validation.application.PossiblyAlreadyFinalizedException;
import ch.adeon.apps.docextract.validation.application.ProcessExpiredException;
import ch.adeon.apps.docextract.validation.port.DmsWriteFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(InvalidAttributesException.class)
  public ProblemDetail handleInvalidAttributes(InvalidAttributesException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    problem.setTitle("Invalid attributes — validation must be repeated");
    return problem;
  }

  @ExceptionHandler(UploadRejectedException.class)
  public ProblemDetail handleUploadRejected(UploadRejectedException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problem.setTitle("Upload rejected");
    return problem;
  }

  // Same status for "unknown" and "other tenant's process" (NfA-4).
  @ExceptionHandler(ProcessNotFoundException.class)
  public ProblemDetail handleProcessNotFound(ProcessNotFoundException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setTitle("Process not found");
    return problem;
  }

  @ExceptionHandler(ProcessExpiredException.class)
  public ProblemDetail handleProcessExpired(ProcessExpiredException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.GONE, ex.getMessage());
    problem.setTitle("Process expired — upload the document again");
    return problem;
  }

  @ExceptionHandler(AlreadyFinalizedException.class)
  public ProblemDetail handleAlreadyFinalized(AlreadyFinalizedException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setTitle("Process already finalized");
    return problem;
  }

  @ExceptionHandler(PossiblyAlreadyFinalizedException.class)
  public ProblemDetail handlePossiblyAlreadyFinalized(PossiblyAlreadyFinalizedException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setTitle("Document possibly already stored in the DMS");
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

  @ExceptionHandler({
    DmsChunkUploadException.class,
    PreviewRenderingException.class,
    DmsWriteFailedException.class
  })
  public ProblemDetail handleUpstreamFailure(RuntimeException ex) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
    problem.setTitle("Upstream service failure");
    return problem;
  }
}
