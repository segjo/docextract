package ch.adeon.apps.docextract.validation.application;

/**
 * Where a validated value comes from (ADR-016, decision 6): {@code LLM} = not set by the DMS (the
 * suggestion, or what a human typed), {@code DMS} = set by a hook or default of the DMS, not
 * derived from the document.
 */
public enum AttributeOrigin {
  LLM,
  DMS
}
