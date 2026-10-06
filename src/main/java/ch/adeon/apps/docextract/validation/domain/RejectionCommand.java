package ch.adeon.apps.docextract.validation.domain;

/** A Sachbearbeiter:in rejects the current suggestion for {@code processId} (FR-5). */
public record RejectionCommand(String processId) {}
