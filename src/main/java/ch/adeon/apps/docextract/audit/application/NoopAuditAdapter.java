package ch.adeon.apps.docextract.audit.application;

import org.springframework.stereotype.Component;

import ch.adeon.apps.docextract.audit.domain.AuditEvent;

@Component
public class NoopAuditAdapter implements AuditPort {

    @Override
    public void append(AuditEvent event) {
        // append-only audit adapter placeholder
    }
}
