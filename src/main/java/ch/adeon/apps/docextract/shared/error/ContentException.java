package ch.adeon.apps.docextract.shared.error;

/** Raised when text/content extraction fails (e.g. PDFBox/docling error, T-4 timeout). */
public class ContentException extends ProcessingFailedException {

  public ContentException(String message, Throwable cause) {
    super(message, cause);
  }

  public ContentException(String message) {
    super(message, null);
  }
}
