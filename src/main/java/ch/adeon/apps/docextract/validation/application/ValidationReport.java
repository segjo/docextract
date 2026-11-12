package ch.adeon.apps.docextract.validation.application;

import ch.adeon.apps.docextract.validation.port.StoreValidationError;
import java.util.List;

/**
 * Result of a DMS check (ADR-016, decision 5): {@code attributes} only if valid, {@code error} only
 * if invalid. Values and the DMS message are for the authorised caller only (C-4).
 */
public record ValidationReport(
    boolean valid, List<ValidatedAttribute> attributes, StoreValidationError error) {

  public static ValidationReport valid(List<ValidatedAttribute> attributes) {
    return new ValidationReport(true, attributes, null);
  }

  public static ValidationReport invalid(StoreValidationError error) {
    return new ValidationReport(false, null, error);
  }
}
