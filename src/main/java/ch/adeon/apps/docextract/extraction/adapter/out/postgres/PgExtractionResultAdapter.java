package ch.adeon.apps.docextract.extraction.adapter.out.postgres;

import ch.adeon.apps.docextract.extraction.application.ExtractionResultPort;
import ch.adeon.apps.docextract.extraction.domain.ExtractedAttribute;
import ch.adeon.apps.docextract.extraction.domain.ExtractionResult;
import java.sql.ResultSet;
import java.sql.SQLException;
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
 * Persists the extraction result in Postgres, not per-instance memory, for the same cluster-mode
 * reason as {@code retrieval.adapter.out.postgres.PgDocumentTypeCandidatesAdapter} (ADR-004). One
 * row per process, replaced wholesale on each store.
 */
@Component
public class PgExtractionResultAdapter implements ExtractionResultPort {

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final Duration ttl;

  public PgExtractionResultAdapter(
      JdbcTemplate jdbcTemplate,
      ObjectMapper objectMapper,
      @Value("${docextract.extraction.result-ttl:PT2H}") Duration ttl) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.ttl = ttl;
  }

  @Override
  public void store(String processId, ExtractionResult result) {
    jdbcTemplate.update("DELETE FROM extraction_result WHERE process_id = ?", processId);
    String json = objectMapper.writeValueAsString(result.attributes());
    Timestamp expiresAt = Timestamp.from(Instant.now().plus(ttl));
    jdbcTemplate.update(
        """
        INSERT INTO extraction_result (process_id, document_type_id, attributes, expires_at)
        VALUES (?, ?, ?::jsonb, ?)
        """,
        processId,
        result.documentTypeId(),
        json,
        expiresAt);
  }

  @Override
  public Optional<ExtractionResult> find(String processId) {
    List<ExtractionResult> rows =
        jdbcTemplate.query(
            "SELECT document_type_id, attributes FROM extraction_result WHERE process_id = ? AND"
                + " expires_at > now()",
            (ResultSet rs, int rowNum) -> mapRow(rs),
            processId);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  private ExtractionResult mapRow(ResultSet rs) throws SQLException {
    CollectionType listType =
        objectMapper.getTypeFactory().constructCollectionType(List.class, ExtractedAttribute.class);
    List<ExtractedAttribute> attributes =
        objectMapper.readValue(rs.getString("attributes"), listType);
    return new ExtractionResult(rs.getString("document_type_id"), attributes);
  }
}
