package ch.adeon.apps.docextract.ingest.application;

import ch.adeon.apps.docextract.shared.error.AdapterUnavailableException;
/** Thrown when the d.velop RepoApp's repositories endpoint cannot be reached or errors. */
public class DmsRepositoryException extends AdapterUnavailableException {

  public DmsRepositoryException(String message, Throwable cause) {
    super(message, cause);
  }
}
