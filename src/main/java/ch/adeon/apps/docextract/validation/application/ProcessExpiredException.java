package ch.adeon.apps.docextract.validation.application;

/** The process's quarantined embedding has passed its TTL; confirm is no longer allowed (C-7). */
public class ProcessExpiredException extends RuntimeException {

  public ProcessExpiredException(String processId) {
    super("Process " + processId + " has expired");
  }
}
