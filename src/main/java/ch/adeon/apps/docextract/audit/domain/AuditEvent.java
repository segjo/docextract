package ch.adeon.apps.docextract.audit.domain;

public record AuditEvent(String eventType, String promptHash, int tokenCount) {
}
