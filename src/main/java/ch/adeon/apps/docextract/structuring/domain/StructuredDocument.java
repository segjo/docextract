package ch.adeon.apps.docextract.structuring.domain;

import java.util.List;

/**
 * Result of structuring one document into contextualized chunks (ADR-006/-007). The chunks are also
 * staged in-memory (see {@code ChunkStagingPort}) for the retrieval/extraction steps to consume;
 * this record is returned to the caller for immediate use (e.g. tests, SSE payload).
 */
public record StructuredDocument(String processId, List<DocChunk> chunks) {}
