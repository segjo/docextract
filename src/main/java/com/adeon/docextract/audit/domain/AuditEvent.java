package com.adeon.docextract.audit.domain;

public record AuditEvent(String eventType, String promptHash, int tokenCount) {
}
