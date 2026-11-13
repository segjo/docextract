package ch.adeon.apps.docextract.validation.application;

import java.util.List;

/**
 * One property value to check. Exactly one of {@code value}/{@code values} carries content; {@code
 * origin} {@code null} counts as {@link AttributeOrigin#LLM}.
 */
public record CandidateAttribute(
    String propertyId, String value, List<String> values, AttributeOrigin origin) {}
