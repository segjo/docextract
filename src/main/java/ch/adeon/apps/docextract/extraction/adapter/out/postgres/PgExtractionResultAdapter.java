package ch.adeon.apps.docextract.extraction.adapter.out.postgres;

import ch.adeon.apps.docextract.extraction.application.ExtractedAttribute;
import ch.adeon.apps.docextract.extraction.application.ExtractionResult;
import ch.adeon.apps.docextract.extraction.application.ExtractionResultPort;
import ch.adeon.apps.docextract.extraction.application.ValidationOutcome;
import ch.adeon.apps.docextract.shared.cleanup.TtlPurger;
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
public class PgExtractionResultAdapter implements ExtractionResultPort, TtlPurger {

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
  public void store(String tenantId, String processId, ExtractionResult result) {
    jdbcTemplate.update(
        "DELETE FROM extraction_result WHERE process_id = ? AND tenant_id = ?",
        processId,
        tenantId);
    String json = objectMapper.writeValueAsString(result.attributes());
    String validation = objectMapper.writeValueAsString(result.validation());
    Timestamp expiresAt = Timestamp.from(Instant.now().plus(ttl));
    jdbcTemplate.update(
        """
        INSERT INTO extraction_result
            (tenant_id, process_id, document_type_id, attributes, validation, expires_at)
        VALUES (?, ?, ?, ?::jsonb, ?::jsonb, ?)
        """,
        tenantId,
        processId,
        result.documentTypeId(),
        json,
        validation,
        expiresAt);
  }

  @Override
  public Optional<ExtractionResult> find(String tenantId, String processId) {
    List<ExtractionResult> rows =
        jdbcTemplate.query(
            "SELECT document_type_id, attributes, validation FROM extraction_result"
                + " WHERE process_id = ? AND tenant_id = ? AND expires_at > now()",
            (ResultSet rs, int rowNum) -> mapRow(rs),
            processId,
            tenantId);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  private ExtractionResult mapRow(ResultSet rs) throws SQLException {
    CollectionType listType =
        objectMapper.getTypeFactory().constructCollectionType(List.class, ExtractedAttribute.class);
    List<ExtractedAttribute> attributes =
        objectMapper.readValue(rs.getString("attributes"), listType);
    // Rows from before ADR-016 have no validation: ExtractionResult reads null as NOT_CHECKED.
    String validationJson = rs.getString("validation");
    ValidationOutcome validation =
        validationJson == null
            ? null
            : objectMapper.readValue(validationJson, ValidationOutcome.class);
    return new ExtractionResult(rs.getString("document_type_id"), attributes, validation);
  }

  @Override
  public int purgeExpired() {
    return jdbcTemplate.update("DELETE FROM extraction_result WHERE expires_at < now()");
  }
}
