package ch.adeon.apps.docextract.audit.adapter.out.postgres;

import ch.adeon.apps.docextract.audit.application.AuditEvent;
import ch.adeon.apps.docextract.audit.application.AuditPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Insert-only into {@code audit_entry}; a DB trigger rejects UPDATE/DELETE (C-4). */
@Component
public class PgAuditAdapter implements AuditPort {

  private final JdbcTemplate jdbcTemplate;

  public PgAuditAdapter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void append(AuditEvent event) {
    jdbcTemplate.update(
        """
        INSERT INTO audit_entry
            (tenant_id, channel, actor_kind, actor_hash, event_type, args_hash, token_count,
             provider, model_id)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        event.tenantId(),
        event.channel().name(),
        event.actorKind() == null ? null : event.actorKind().name(),
        event.actorHash(),
        event.eventType(),
        event.argsHash(),
        event.tokenCount(),
        event.provider(),
        event.modelId());
  }
}
