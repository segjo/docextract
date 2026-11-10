package ch.adeon.apps.docextract.shared.adapter.web;

import ch.adeon.apps.docextract.shared.error.DocextractException;
import ch.adeon.apps.docextract.shared.error.LimitExceededException;
import ch.adeon.apps.docextract.shared.error.ProblemCode;
import ch.adeon.apps.docextract.shared.error.RangeNotSatisfiableException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
/**
 * The single place that turns failures into {@code application/problem+json} (RFC 9457,
 * ARCHITECTURE.md § 8.1). {@code type} is {@code urn:docextract:problem:<code>}; {@code detail}
 * never carries PII (C-4). The MCP tool uses the same {@link ProblemCode}s.
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private static final String SPA_PREFIX = "/ui/";
  private static final URI ABOUT_BLANK = URI.create("about:blank");

  @ExceptionHandler(DocextractException.class)
  public ResponseEntity<ProblemDetail> handleDocextract(DocextractException ex) {
    HttpStatus status = statusOf(ex);
    if (status.is5xxServerError()) {
      log.warn("request failed code={}", ex.code().slug(), ex);
    }
    ProblemDetail problem = ProblemDetails.of(ex.code(), status, ex.detail());
    if (ex.processId() != null) {
      problem.setProperty("processId", ex.processId());
    }
    ResponseEntity.BodyBuilder response = ResponseEntity.status(status);
    if (ex instanceof RangeNotSatisfiableException range) {
      response.header(HttpHeaders.CONTENT_RANGE, "bytes */" + range.totalSize());
    }
    return response.body(problem);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) throws Exception {
    // Spring Security's filter chain turns these into 401/403.
    if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
      throw ex;
    }
    log.error("unhandled exception", ex);
    ProblemDetail problem =
        ProblemDetails.of(
            ProblemCode.INTERNAL_ERROR,
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
  }

  @Override
  protected ResponseEntity<Object> handleNoResourceFoundException(
      NoResourceFoundException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    // Client-side routes of the SPA have no static resource: leave the 404 to the error dispatch,
    // which SpaFallbackErrorController forwards to index.html.
    if (request instanceof ServletWebRequest servletRequest
        && servletRequest.getRequest().getRequestURI().contains(SPA_PREFIX)) {
      try {
        servletRequest.getResponse().sendError(HttpServletResponse.SC_NOT_FOUND);
      } catch (IOException ioException) {
        log.debug("could not send 404", ioException);
      }
      return null;
    }
    return super.handleNoResourceFoundException(ex, headers, status, request);
  }

  @Override
  protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
      MaxUploadSizeExceededException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    ProblemDetail problem =
        ProblemDetails.of(
            ProblemCode.LIMIT_EXCEEDED,
            HttpStatus.PAYLOAD_TOO_LARGE,
            "The request exceeds the maximum upload size");
    return createResponseEntity(problem, headers, HttpStatus.PAYLOAD_TOO_LARGE, request);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex,
      Object body,
      HttpHeaders headers,
      HttpStatusCode statusCode,
      WebRequest request) {
    ResponseEntity<Object> response =
        super.handleExceptionInternal(ex, body, headers, statusCode, request);
    if (response != null
        && response.getBody() instanceof ProblemDetail problem
        && (problem.getType() == null || ABOUT_BLANK.equals(problem.getType()))) {
      problem.setType(URI.create(ProblemDetails.codeFor(statusCode).typeUri()));
      if (ex instanceof TypeMismatchException mismatch) {
        // The default detail echoes the rejected value.
        String name =
            mismatch instanceof MethodArgumentTypeMismatchException argument
                ? argument.getName()
                : mismatch.getPropertyName();
        problem.setDetail(
            name == null ? "Invalid parameter value" : "Invalid value for '" + name + "'");
      }
    }
    return response;
  }

  private static HttpStatus statusOf(DocextractException ex) {
    return switch (ex.code()) {
      case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
      case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
      case FORBIDDEN -> HttpStatus.FORBIDDEN;
      case NOT_FOUND -> HttpStatus.NOT_FOUND;
      case PROCESS_EXPIRED -> HttpStatus.GONE;
      case CONFLICT_STATE -> HttpStatus.CONFLICT;
      case INVALID_ATTRIBUTES -> HttpStatus.UNPROCESSABLE_ENTITY;
      case LIMIT_EXCEEDED ->
          ex instanceof LimitExceededException limit
                  && limit.limit() == LimitExceededException.Limit.PAGE_COUNT
              ? HttpStatus.UNPROCESSABLE_ENTITY
              : HttpStatus.PAYLOAD_TOO_LARGE;
      case RANGE_NOT_SATISFIABLE -> HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE;
      case ADAPTER_UNAVAILABLE -> HttpStatus.BAD_GATEWAY;
      case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }
}
