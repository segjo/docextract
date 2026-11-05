package ch.adeon.apps.docextract.validation.application;

import ch.adeon.apps.docextract.validation.domain.FinalizationStatus;

/** {@code indexStatus} shows whether the corpus promotion is still outstanding (ADR-015). */
public record ConfirmationResult(
    String repositoryId, String documentId, boolean writtenBack, FinalizationStatus indexStatus) {}
