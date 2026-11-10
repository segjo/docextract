package ch.adeon.apps.docextract.shared.adapter.web;

import ch.adeon.apps.docextract.shared.error.ProblemCode;
import java.net.URI;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
/** Builds the one error shape of the REST API: RFC 9457 with a {@code urn:docextract:problem}. */
public final class ProblemDetails {

  private ProblemDetails() {}

  public static ProblemDetail of(ProblemCode code, HttpStatusCode status, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setType(URI.create(code.typeUri()));
    problem.setTitle(code.title());
    return problem;
  }

  /** The type a framework-generated problem gets, derived from its HTTP status. */
  static ProblemCode codeFor(HttpStatusCode status) {
    return switch (status.value()) {
      case 401 -> ProblemCode.UNAUTHORIZED;
      case 403 -> ProblemCode.FORBIDDEN;
      case 404 -> ProblemCode.NOT_FOUND;
      case 413 -> ProblemCode.LIMIT_EXCEEDED;
      case 416 -> ProblemCode.RANGE_NOT_SATISFIABLE;
      default ->
          status.is4xxClientError() ? ProblemCode.INVALID_REQUEST : ProblemCode.INTERNAL_ERROR;
    };
  }
}
