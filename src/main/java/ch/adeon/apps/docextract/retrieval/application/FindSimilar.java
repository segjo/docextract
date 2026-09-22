package ch.adeon.apps.docextract.retrieval.application;

import ch.adeon.apps.docextract.retrieval.domain.RetrievalResult;
import ch.adeon.apps.docextract.security.domain.DvelopCredential;
import java.util.List;

/**
 * Inbound port: embeds+stages a process's chunks and searches the {@code APPROVED} corpus for the
 * top-K most similar documents (FR-3, NfA-6). {@code credential} is passed explicitly (not read
 * from SecurityContextHolder) since this runs off the request thread and is replayed on the
 * outbound DMS object-properties lookup for each hit.
 */
public interface FindSimilar {

  List<RetrievalResult> find(String processId, int topK, DvelopCredential credential);
}
