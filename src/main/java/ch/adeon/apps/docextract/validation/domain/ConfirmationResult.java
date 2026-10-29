package ch.adeon.apps.docextract.validation.domain;

public record ConfirmationResult(
    String repositoryId, String documentId, boolean writtenBack) {}
