package ch.adeon.apps.docextract.content.domain;

/**
 * Document representation for FR-4 (attribute extraction), configured independently of {@link
 * ExtractedText}'s first-N-words limit for FR-3 (SPEC §2 "offen gelassen": representation is a
 * per-adapter decision, e.g. full text, Markdown, or page images). Held only in-memory (ADR-006) —
 * never persisted.
 */
public record DocumentContent(Representation representation, String value) {}
