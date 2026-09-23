package ch.adeon.apps.docextract.extraction.application;

/** Thrown when the extraction LLM call fails (FR-4). */
public class LlmException extends RuntimeException {

  public LlmException(String message) {
    super(message);
  }

  public LlmException(String message, Throwable cause) {
    super(message, cause);
  }
}
