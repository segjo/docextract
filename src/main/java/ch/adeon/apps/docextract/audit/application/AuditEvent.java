package ch.adeon.apps.docextract.audit.application;

public record AuditEvent(String eventType, String promptHash, int tokenCount) {
}
