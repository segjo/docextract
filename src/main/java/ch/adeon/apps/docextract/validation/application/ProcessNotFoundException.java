package ch.adeon.apps.docextract.validation.application;

/**
 * Process unknown, already cleaned up, or owned by another tenant — deliberately indistinguishable
 * (NfA-4).
 */
public class ProcessNotFoundException extends RuntimeException {

  public ProcessNotFoundException(String processId) {
    super("Process " + processId + " not found");
  }
}
