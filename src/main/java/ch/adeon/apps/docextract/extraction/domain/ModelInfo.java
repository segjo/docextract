package ch.adeon.apps.docextract.extraction.domain;

/**
 * Identifies which model produced an {@link LlmResponse} (C-4/NfA-7 audit trail, §8.5). {@code
 */
public record ModelInfo(String provider, String modelId) {}
