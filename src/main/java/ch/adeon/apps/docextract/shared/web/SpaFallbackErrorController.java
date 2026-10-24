package ch.adeon.apps.docextract.shared.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * The Angular SPA is served as static content under {@code /ui/**} (see {@link WebMvcConfig}),
 * but deep links into client-side routes (e.g. {@code /ui/processes/{id}}) have no matching
 * static resource. Spring's {@code PathPattern} parser rejects a {@code **} segment placed in
 * the middle of a view-controller mapping, so instead of mapping those routes directly we
 * intercept the resulting 404 here and forward it to {@code index.html} so the client-side
 * router can take over. Requests for actual missing files (last path segment contains a dot)
 * are left as plain 404s.
 */
@Controller
public class SpaFallbackErrorController implements ErrorController {

  @GetMapping("/error")
  public Object handleError(HttpServletRequest request) {
    Object statusAttribute = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    Object uriAttribute = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
    String requestUri = uriAttribute == null ? "" : uriAttribute.toString();
    String contextPath = request.getContextPath();
    String pathWithinApp =
        contextPath != null && requestUri.startsWith(contextPath)
            ? requestUri.substring(contextPath.length())
            : requestUri;

    boolean isNotFound =
        statusAttribute instanceof Integer status && status == HttpStatus.NOT_FOUND.value();
    boolean isSpaRoute = pathWithinApp.startsWith("/ui/") && !lastSegmentLooksLikeFile(pathWithinApp);

    if (isNotFound && isSpaRoute) {
      return new ModelAndView("forward:/ui/index.html");
    }

    HttpStatus status =
        statusAttribute instanceof Integer code
            ? HttpStatus.valueOf(code)
            : HttpStatus.INTERNAL_SERVER_ERROR;
    return ResponseEntity.status(status).build();
  }

  private boolean lastSegmentLooksLikeFile(String requestUri) {
    int lastSlash = requestUri.lastIndexOf('/');
    String lastSegment = requestUri.substring(lastSlash + 1);
    return lastSegment.contains(".");
  }
}
