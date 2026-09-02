package com.adeon.docextract.security.domain;

public record AuthContext(String tenantId, String aclRef, String userId) {
}
