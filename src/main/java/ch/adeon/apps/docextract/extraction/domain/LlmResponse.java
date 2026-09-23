package ch.adeon.apps.docextract.extraction.domain;

/**
 * Raw result of one {@link ch.adeon.apps.docextract.extraction.application.LlmPort} call: {@code
 * json} is the model's schema-constrained JSON output (not yet parsed into a domain type — callers
 * know the schema they asked for), {@code model} and {@code promptTokenCount} are carried through
 * for the audit trail (C-4, NfA-7).
 */
public record LlmResponse(String json, String model, int promptTokenCount) {}
