package ch.adeon.apps.docextract.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;

class SpaFallbackErrorControllerTest {

  private final SpaFallbackErrorController controller = new SpaFallbackErrorController();

  @Test
  void forwardsSpaDeepLinkNotFoundToIndexHtml() {
    HttpServletRequest request =
        requestFor("/adeon-docextract", "/adeon-docextract/ui/processes/60df9827", 404);

    Object result = controller.handleError(request);

    assertThat(result).isInstanceOf(ModelAndView.class);
    assertThat(((ModelAndView) result).getViewName()).isEqualTo("forward:/ui/index.html");
  }

  @Test
  void leavesMissingStaticAssetAsPlain404() {
    HttpServletRequest request =
        requestFor("/adeon-docextract", "/adeon-docextract/ui/no-such-file.js", 404);

    Object result = controller.handleError(request);

    assertThat(result).isInstanceOf(ResponseEntity.class);
    assertThat(((ResponseEntity<?>) result).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void leavesNonUiNotFoundAsPlain404() {
    HttpServletRequest request =
        requestFor("/adeon-docextract", "/adeon-docextract/api/v1/unknown", 404);

    Object result = controller.handleError(request);

    assertThat(result).isInstanceOf(ResponseEntity.class);
    assertThat(((ResponseEntity<?>) result).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void leavesNonNotFoundErrorsUntouched() {
    HttpServletRequest request =
        requestFor("/adeon-docextract", "/adeon-docextract/ui/processes/60df9827", 500);

    Object result = controller.handleError(request);

    assertThat(result).isInstanceOf(ResponseEntity.class);
    assertThat(((ResponseEntity<?>) result).getStatusCode())
        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private HttpServletRequest requestFor(String contextPath, String requestUri, int status) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getContextPath()).thenReturn(contextPath);
    when(request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI)).thenReturn(requestUri);
    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(status);
    return request;
  }
}
