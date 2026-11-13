package ch.adeon.apps.docextract.validation.application;

import java.util.List;

/**
 * A property as the DMS would store it. {@code changedByDms}: the DMS set or changed the value
 * (also {@code true} for {@link AttributeOrigin#DMS}).
 */
public record ValidatedAttribute(
    String propertyId,
    String value,
    List<String> values,
    AttributeOrigin origin,
    boolean changedByDms) {}
