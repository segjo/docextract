package ch.adeon.apps.docextract.agentgateway.application;

import java.util.List;

/** Extraction result of a process as handed to an agent; empty while extraction hasn't run. */
public record ProcessSuggestions(
    String processId, String documentTypeId, List<AttributeSuggestion> attributes) {

  /** {@code confidence} is {@code null} for a value the DMS set (ADR-016). */
  public record AttributeSuggestion(
      String propertyId, String value, List<String> values, Double confidence) {}
}
