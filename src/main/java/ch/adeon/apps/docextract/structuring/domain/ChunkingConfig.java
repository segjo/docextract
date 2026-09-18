package ch.adeon.apps.docextract.structuring.domain;

/**
 * Chunk-size budget applied while splitting structured content into {@link DocChunk}s. Per ADR-007
 * this is ultimately a property of the embedding model (tokenizer alignment); until the retrieval
 * module exposes a real {@code ChunkingConfigPort} backed by the embedding model's tokenizer,
 * {@code maxTokens} is approximated by word count and configured via {@code
 * docextract.structuring.max-chunk-tokens}.
 */
public record ChunkingConfig(int maxTokens) {

  public ChunkingConfig {
    if (maxTokens <= 0) {
      throw new IllegalArgumentException("maxTokens must be positive");
    }
  }
}
