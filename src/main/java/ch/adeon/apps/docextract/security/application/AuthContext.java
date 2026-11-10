package ch.adeon.apps.docextract.security.application;

/**
 * {@code agent}: the caller belongs to the configured agent group (§ 8.6). {@code actorHash} is the
 * keyed pseudonym of the caller for the audit (C-4), never the raw user id.
 */
public record AuthContext(
    String tenantId, String userId, String displayName, boolean agent, String actorHash) {

  // Without a resolved actor key (tests); the real adapter always supplies the hash.
  public AuthContext(String tenantId, String userId, String displayName, boolean agent) {
    this(tenantId, userId, displayName, agent, null);
  }

  public AuthContext(String tenantId, String userId, String displayName) {
    this(tenantId, userId, displayName, false, null);
  }
}
