package ch.adeon.apps.docextract.validation.port;

/**
 * Outbound port deleting a process's staged {@code PENDING} embedding on rejection, final failure,
 * or TTL expiry (ADR-006/-012, C-7, T-2) — ungeprüfte Dokumente dürfen den Korpus nie beeinflussen.
 */
public interface PendingEmbeddingDeletePort {

  void delete(String processId);
}
