package ch.adeon.apps.docextract.security.application;

public record AuthContext(String tenantId, String userId, String displayName) {}
