package ch.adeon.apps.docextract.shared.error;

/**
 * Unknown, expired or owned by another tenant/user — deliberately indistinguishable (NfA-4), so
 * there is no separate tenant-mismatch type.
 */
public class NotFoundException extends DocextractException {

  public NotFoundException(String message, String processId) {
    super(ProblemCode.NOT_FOUND, message, message, processId, null);
  }
}
