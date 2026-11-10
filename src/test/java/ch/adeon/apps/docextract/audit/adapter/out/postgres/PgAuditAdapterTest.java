package ch.adeon.apps.docextract.audit.adapter.out.postgres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.adeon.apps.docextract.audit.application.AuditChannel;
import ch.adeon.apps.docextract.audit.application.AuditEvent;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class PgAuditAdapterTest {

  @Container
  @SuppressWarnings("resource") // lifecycle managed by the @Testcontainers JUnit extension
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(
              DockerImageName.parse("pgvector/pgvector:pg17-bookworm")
                  .asCompatibleSubstituteFor("postgres"))
          .withDatabaseName("docextract")
          .withUsername("docextract")
          .withPassword("docextract-test");

  private static JdbcTemplate jdbcTemplate;

  @BeforeAll
  static void migrate() {
    DataSource dataSource =
        DataSourceBuilder.create()
            .url(POSTGRES.getJdbcUrl())
            .username(POSTGRES.getUsername())
            .password(POSTGRES.getPassword())
            .driverClassName("org.postgresql.Driver")
            .build();
    new JdbcTemplate(dataSource).execute("CREATE EXTENSION IF NOT EXISTS vector");
    Flyway.configure().dataSource(dataSource).load().migrate();
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @Test
  void agent_call_on_mcp_is_stored_with_tenant_channel_actor_kind_and_hashes() {
    new PgAuditAdapter(jdbcTemplate)
        .append(
            AuditEvent.byUser(
                "tenant-a",
                AuditChannel.MCP,
                true,
                "a".repeat(64),
                "mcp.tool.find_similar_documents.ok",
                "b".repeat(64)));

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "SELECT * FROM audit_entry WHERE event_type = 'mcp.tool.find_similar_documents.ok'");
    assertThat(row)
        .containsEntry("tenant_id", "tenant-a")
        .containsEntry("channel", "MCP")
        .containsEntry("actor_kind", "AGENT")
        .containsEntry("actor_hash", "a".repeat(64))
        .containsEntry("args_hash", "b".repeat(64))
        .containsEntry("token_count", 0);
    assertThat(row.get("created_at")).isNotNull();
  }

  @Test
  void person_in_the_ui_is_stored_as_human() {
    new PgAuditAdapter(jdbcTemplate)
        .append(
            AuditEvent.byUser(
                "tenant-a", AuditChannel.UI, false, "c".repeat(64), "validation.reject", null));

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "SELECT * FROM audit_entry WHERE event_type = 'validation.reject'");
    assertThat(row).containsEntry("channel", "UI").containsEntry("actor_kind", "HUMAN");
  }

  @Test
  void ttl_cleanup_is_the_only_event_without_tenant_and_actor() {
    new PgAuditAdapter(jdbcTemplate).append(AuditEvent.ttlCleanup());

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "SELECT * FROM audit_entry WHERE event_type = 'validation.ttl-cleanup'");
    assertThat(row)
        .containsEntry("channel", "SYSTEM")
        .containsEntry("tenant_id", null)
        .containsEntry("actor_kind", null)
        .containsEntry("actor_hash", null)
        .containsEntry("provider", null)
        .containsEntry("model_id", null);
  }

  @Test
  void llm_event_is_stored_with_tenant_provider_model_and_tokens_but_no_actor() {
    new PgAuditAdapter(jdbcTemplate)
        .append(
            AuditEvent.llm(
                "tenant-a",
                "extraction.classify",
                "d".repeat(64),
                42,
                "openai-compatible",
                "qwen3"));

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "SELECT * FROM audit_entry WHERE event_type = 'extraction.classify'");
    assertThat(row)
        .containsEntry("tenant_id", "tenant-a")
        .containsEntry("channel", "SYSTEM")
        .containsEntry("actor_kind", null)
        .containsEntry("actor_hash", null)
        .containsEntry("args_hash", "d".repeat(64))
        .containsEntry("token_count", 42)
        .containsEntry("provider", "openai-compatible")
        .containsEntry("model_id", "qwen3");
  }

  @Test
  void entries_are_append_only() {
    new PgAuditAdapter(jdbcTemplate).append(AuditEvent.system("tenant-a", "validation.retry"));

    assertThatThrownBy(() -> jdbcTemplate.update("UPDATE audit_entry SET event_type = 'x'"))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("append-only");
    assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM audit_entry"))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("append-only");
  }

  @Test
  void unknown_channel_or_actor_kind_is_rejected_by_the_database() {
    assertThatThrownBy(() ->
            jdbcTemplate.update(
                "INSERT INTO audit_entry (channel, event_type) VALUES ('AGENT', 'x')"))
        .isInstanceOf(DataAccessException.class);
    assertThatThrownBy(() ->
            jdbcTemplate.update(
                "INSERT INTO audit_entry (channel, actor_kind, event_type)"
                    + " VALUES ('MCP', 'ROBOT', 'x')"))
        .isInstanceOf(DataAccessException.class);
  }

  @Test
  void table_has_no_column_for_raw_values() {
    List<String> columns =
        jdbcTemplate.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'audit_entry'",
            String.class);

    assertThat(columns)
        .containsExactlyInAnyOrder(
            "id",
            "created_at",
            "tenant_id",
            "channel",
            "actor_kind",
            "actor_hash",
            "event_type",
            "args_hash",
            "token_count",
            "provider",
            "model_id");
  }
}
