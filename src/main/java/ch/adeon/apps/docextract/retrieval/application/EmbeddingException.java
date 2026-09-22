package ch.adeon.apps.docextract.retrieval.application;

/** Thrown when the embedding model call fails (FR-3). */
public class EmbeddingException extends RuntimeException {

  public EmbeddingException(String message, Throwable cause) {
    super(message, cause);
  }

  public EmbeddingException(String message) {
    super(message);
  }
}
