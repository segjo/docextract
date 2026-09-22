package ch.adeon.apps.docextract.retrieval.adapter.out.postgres;

import ch.adeon.apps.docextract.retrieval.application.DocumentTypeCandidatesPort;
import ch.adeon.apps.docextract.retrieval.domain.DmsDocumentMetadata;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.type.CollectionType;

/**
 * Persists the document type candidates fetched as a fallback (SPEC §3) in Postgres, not
 * per-instance memory, for the same cluster-mode reason as {@link PgRetrievalResultAdapter}
 * (ADR-004). One row per process, replaced wholesale on each store.
 */
@Component
public class PgDocumentTypeCandidatesAdapter implements DocumentTypeCandidatesPort {

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final Duration ttl;

  public PgDocumentTypeCandidatesAdapter(
      JdbcTemplate jdbcTemplate,
      ObjectMapper objectMapper,
      @Value("${docextract.retrieval.result-ttl:PT2H}") Duration ttl) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.ttl = ttl;
  }

  @Override
  public void store(String processId, List<DmsDocumentMetadata> candidates) {
    jdbcTemplate.update("DELETE FROM document_type_candidate WHERE process_id = ?", processId);
    if (candidates.isEmpty()) {
      return;
    }
    String json = objectMapper.writeValueAsString(candidates);
    Timestamp expiresAt = Timestamp.from(Instant.now().plus(ttl));
    jdbcTemplate.update(
        """
        INSERT INTO document_type_candidate (process_id, candidates, expires_at)
        VALUES (?, ?::jsonb, ?)
        """,
        processId,
        json,
        expiresAt);
  }

  @Override
  public Optional<List<DmsDocumentMetadata>> find(String processId) {
    List<String> rows =
        jdbcTemplate.queryForList(
            "SELECT candidates FROM document_type_candidate WHERE process_id = ? AND expires_at >"
                + " now()",
            String.class,
            processId);
    if (rows.isEmpty()) {
      return Optional.empty();
    }
    CollectionType listType =
        objectMapper
            .getTypeFactory()
            .constructCollectionType(List.class, DmsDocumentMetadata.class);
    return Optional.of(objectMapper.readValue(rows.get(0), listType));
  }
}
