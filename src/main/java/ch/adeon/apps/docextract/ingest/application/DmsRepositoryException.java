package ch.adeon.apps.docextract.ingest.application;

/** Thrown when the d.velop RepoApp's repositories endpoint cannot be reached or errors. */
public class DmsRepositoryException extends RuntimeException {

  public DmsRepositoryException(String message, Throwable cause) {
    super(message, cause);
  }
}
