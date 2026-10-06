package ch.adeon.apps.docextract.content.domain;

/** Raised when text/content extraction fails (e.g. PDFBox/docling error, T-4 timeout). */
public class ContentException extends RuntimeException {

  public ContentException(String message, Throwable cause) {
    super(message, cause);
  }

  public ContentException(String message) {
    super(message);
  }
}
