package ch.adeon.apps.docextract.security.domain;

public record AuthContext(String tenantId, String userId, String displayName) {}
