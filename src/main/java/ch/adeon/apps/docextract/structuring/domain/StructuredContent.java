package ch.adeon.apps.docextract.structuring.domain;

/**
 * Normalized document text produced by a {@code StructuringPort} adapter, prior to chunking.
 * Markdown-capable adapters (docling) emit {@code #}-style headings so the chunker can derive
 * heading context; plain-text fast-track adapters (PDFBox) emit unstructured text and yield flat
 * chunks without heading context.
 */
public record StructuredContent(String text) {}
