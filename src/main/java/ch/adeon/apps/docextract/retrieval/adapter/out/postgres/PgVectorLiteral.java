package ch.adeon.apps.docextract.retrieval.adapter.out.postgres;

/**
 * Formats an embedding as the textual literal pgvector accepts via an explicit {@code ::vector}
 * cast (e.g. {@code '[0.1,0.2]'::vector}) — avoids depending on the pgvector JDBC extension type
 * for a plain {@link org.springframework.jdbc.core.JdbcTemplate}-only codebase.
 */
final class PgVectorLiteral {

  private PgVectorLiteral() {}

  static String of(float[] values) {
    StringBuilder sb = new StringBuilder(values.length * 8 + 2);
    sb.append('[');
    for (int i = 0; i < values.length; i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(values[i]);
    }
    return sb.append(']').toString();
  }
}
