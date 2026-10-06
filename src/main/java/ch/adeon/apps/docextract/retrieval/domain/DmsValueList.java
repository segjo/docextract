package ch.adeon.apps.docextract.retrieval.domain;

import java.util.List;

/**
 * Result of a {@link ch.adeon.apps.docextract.retrieval.port.ValueListPort} lookup, capped
 * to a configurable maximum (T-4: a value list can hold arbitrarily many entries). {@code hasMore}
 * is {@code true} when the webhook returned more values than fit into {@code values}.
 */
public record DmsValueList(List<String> values, boolean hasMore) {

  public static final DmsValueList EMPTY = new DmsValueList(List.of(), false);
}
