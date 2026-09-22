package ch.adeon.apps.docextract.retrieval.domain;

import java.util.Arrays;

/**
 * A dense embedding vector produced by the configured embedding model (FR-3, §8.5). Deliberately a
 * plain value object (not a record) so equality is content-based rather than array-reference-based.
 */
public final class EmbeddingVector {

  private final float[] values;

  public EmbeddingVector(float[] values) {
    if (values == null || values.length == 0) {
      throw new IllegalArgumentException("embedding vector must not be empty");
    }
    this.values = values.clone();
  }

  public float[] values() {
    return values.clone();
  }

  public int dimensions() {
    return values.length;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof EmbeddingVector other && Arrays.equals(values, other.values);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(values);
  }

  @Override
  public String toString() {
    return "EmbeddingVector[dimensions=" + values.length + "]";
  }
}
