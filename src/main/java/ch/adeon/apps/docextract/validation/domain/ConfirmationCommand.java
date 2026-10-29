package ch.adeon.apps.docextract.validation.domain;

import java.util.Map;

/** {@code dmsSourceCategory} is the DMS document type/category selected by the user. */
public record ConfirmationCommand(
    String processId, String dmsSourceCategory, Map<String, Object> attributes) {}
