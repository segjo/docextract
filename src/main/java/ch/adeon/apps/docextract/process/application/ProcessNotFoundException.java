package ch.adeon.apps.docextract.process.application;

import ch.adeon.apps.docextract.shared.error.NotFoundException;
/**
 * Process unknown, already cleaned up, or owned by another tenant — deliberately indistinguishable
 * (NfA-4).
 */
public class ProcessNotFoundException extends NotFoundException {

  public ProcessNotFoundException(String processId) {
    super("Process " + processId + " not found", processId);
  }
}
