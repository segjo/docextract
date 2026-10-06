package ch.adeon.apps.docextract.validation.domain;

import java.util.Map;

/**
 * {@code templateConsent} is a separate, explicit opt-in for adopting the confirmed attributes as a
 * herkunftsmarkierte Vorlage (FR-5) — it must never be implied by {@code consentGiven} alone.
 */
public record ConfirmationCommand(
    String processId,
    String documentId,
    String repositoryId,
    boolean consentGiven,
    boolean templateConsent,
    Map<String, Object> attributes) {}
