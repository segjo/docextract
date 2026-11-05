package ch.adeon.apps.docextract.audit.application;


public interface AuditPort {
    void append(AuditEvent event);
}
