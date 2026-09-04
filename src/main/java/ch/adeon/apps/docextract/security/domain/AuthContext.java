package ch.adeon.apps.docextract.security.domain;

public record AuthContext(String tenantId, String aclRef, String userId, String displayName) {
}
