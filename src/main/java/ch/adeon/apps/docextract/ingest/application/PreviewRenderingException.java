package ch.adeon.apps.docextract.ingest.application;

import ch.adeon.apps.docextract.shared.error.AdapterUnavailableException;
/** Thrown when Gotenberg cannot render a non-PDF upload into a preview PDF. */
public class PreviewRenderingException extends AdapterUnavailableException {

  public PreviewRenderingException(String message) {
    super(message);
  }

  public PreviewRenderingException(String message, Throwable cause) {
    super(message, cause);
  }
}
