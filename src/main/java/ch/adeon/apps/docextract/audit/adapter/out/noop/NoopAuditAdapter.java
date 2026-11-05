package ch.adeon.apps.docextract.audit.adapter.out.noop;

import ch.adeon.apps.docextract.audit.application.AuditEvent;
import ch.adeon.apps.docextract.audit.application.AuditPort;
import org.springframework.stereotype.Component;

@Component
public class NoopAuditAdapter implements AuditPort {

  @Override
  public void append(AuditEvent event) {
    // append-only audit adapter placeholder
  }
}
