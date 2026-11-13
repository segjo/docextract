package ch.adeon.apps.docextract.validation.port;

import java.util.List;

/**
 * Outcome of a DMS check. {@code Valid.submitted} are the values as sent (after normalisation),
 * {@code Valid.returned} the allowlisted, non-empty properties of the DMS response (empty if the
 * response could not be read).
 */
public sealed interface StoreValidationResult {

  record Valid(List<PropertyValue> submitted, List<PropertyValue> returned)
      implements StoreValidationResult {}

  record Invalid(StoreValidationError error) implements StoreValidationResult {}
}
