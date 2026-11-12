package ch.adeon.apps.docextract.extraction.application;

import ch.adeon.apps.docextract.validation.port.StoreValidationError;

/**
 * Result of the final DMS check of an extraction (ADR-016): {@code VALID}, {@code INVALID} with the
 * one error the DMS reported, or {@code NOT_CHECKED} (DMS could not answer). Never blocks: the
 * values stay in every case.
 */
public record ValidationOutcome(Status status, StoreValidationError error) {

  public enum Status {
    VALID,
    INVALID,
    NOT_CHECKED
  }

  public static final ValidationOutcome VALID = new ValidationOutcome(Status.VALID, null);
  public static final ValidationOutcome NOT_CHECKED =
      new ValidationOutcome(Status.NOT_CHECKED, null);

  public static ValidationOutcome invalid(StoreValidationError error) {
    return new ValidationOutcome(Status.INVALID, error);
  }
}
