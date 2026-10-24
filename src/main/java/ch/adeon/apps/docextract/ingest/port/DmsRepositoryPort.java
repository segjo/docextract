package ch.adeon.apps.docextract.ingest.port;

import ch.adeon.apps.docextract.ingest.domain.DmsRepository;
import ch.adeon.apps.docextract.security.domain.DvelopCredential;
import java.util.List;

/**
 * Outbound port: reads the repository connections the current user can see via the d.velop RepoApp
 * ({@code GET /repo/repositories}, doc/api/dvelop-repoapp.md) — live per call, the requesting
 * user's own session determines which repositories come back as {@code isAvailable}.
 */
public interface DmsRepositoryPort {

  List<DmsRepository> fetchAll(DvelopCredential credential);
}
