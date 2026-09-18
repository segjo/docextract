package ch.adeon.apps.docextract.structuring.domain;

/** Raised when converting or chunking a document fails (e.g. docling/PDFBox error, T-4 timeout). */
public class StructuringException extends RuntimeException {

  public StructuringException(String message, Throwable cause) {
    super(message, cause);
  }

  public StructuringException(String message) {
    super(message);
  }
}
