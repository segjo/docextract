package ch.adeon.apps.docextract.audit.application;

import ch.adeon.apps.docextract.audit.domain.AuditEvent;

public interface AuditPort {
    void append(AuditEvent event);
}
