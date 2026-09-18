package ch.adeon.apps.docextract.structuring.domain;

import java.util.List;

/**
 * A structure- and token-aware chunk (ADR-007). {@code headingPath} carries the section headings
 * the chunk was found under; {@code text} is already contextualized (heading path prefixed) so it
 * is ready to be embedded as-is. Held only in-memory (ADR-006) — never persisted.
 */
public record DocChunk(int index, List<String> headingPath, String text) {}
