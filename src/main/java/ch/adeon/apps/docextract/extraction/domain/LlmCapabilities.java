package ch.adeon.apps.docextract.extraction.domain;

/**
 * Declared capabilities of an {@link ch.adeon.apps.docextract.extraction.port.LlmPort} adapter
 * (ADR-011, §5.2/§8.5): whether the model supports schema-constrained decoding natively, and what
 * input modality it accepts. Server-side JSON-schema validation of the response is always performed
 * regardless of {@code structuredOutput} (T-1).
 */
public record LlmCapabilities(StructuredOutputSupport structuredOutput, InputKind inputs) {

  public enum StructuredOutputSupport {
    NATIVE,
    NONE
  }

  public enum InputKind {
    TEXT,
    IMAGES
  }
}
