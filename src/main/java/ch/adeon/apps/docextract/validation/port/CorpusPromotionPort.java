package ch.adeon.apps.docextract.validation.port;

/**
 * Outbound port promoting a process's staged {@code PENDING} embedding to {@code APPROVED} after
 * consent (ADR-002/-006/-012, C-7). The only writer allowed to move a row out of quarantine into
 * the active retrieval corpus. Attaches the DMS identifiers only known once finalization succeeded.
 */
public interface CorpusPromotionPort {

  void promote(String processId, String repositoryId, String dmsDocumentId);
}
