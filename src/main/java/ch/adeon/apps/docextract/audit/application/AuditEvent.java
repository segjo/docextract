package ch.adeon.apps.docextract.audit.application;

/**
 * Append-only audit entry (C-4): only hashes and references, never raw PII. {@code tenantId},
 * {@code actorKind} and {@code actorHash} are {@code null} only for the TTL cleanup, which has no
 * tenant; {@code argsHash} is the SHA-256 of the arguments or prompt, never their content. {@code
 * provider} and {@code modelId} are set for LLM calls.
 */
public record AuditEvent(
    String tenantId,
    AuditChannel channel,
    ActorKind actorKind,
    String actorHash,
    String eventType,
    String argsHash,
    int tokenCount,
    String provider,
    String modelId) {

  /** The system-wide TTL cleanup, the only event without a tenant. */
  public static AuditEvent ttlCleanup() {
    return new AuditEvent(
        null, AuditChannel.SYSTEM, null, null, "validation.ttl-cleanup", null, 0, null, null);
  }

  /** Event of the system on behalf of a tenant, e.g. the promotion retry. */
  public static AuditEvent system(String tenantId, String eventType) {
    return system(tenantId, eventType, null);
  }

  public static AuditEvent system(String tenantId, String eventType, String argsHash) {
    return new AuditEvent(
        tenantId, AuditChannel.SYSTEM, null, null, eventType, argsHash, 0, null, null);
  }

  /** LLM call of the pipeline on behalf of a tenant. */
  public static AuditEvent llm(
      String tenantId,
      String eventType,
      String promptHash,
      int tokenCount,
      String provider,
      String modelId) {
    return new AuditEvent(
        tenantId,
        AuditChannel.SYSTEM,
        null,
        null,
        eventType,
        promptHash,
        tokenCount,
        provider,
        modelId);
  }

  /** Action of a requester; {@code agent} = member of the agent group. */
  public static AuditEvent byUser(
      String tenantId,
      AuditChannel channel,
      boolean agent,
      String actorHash,
      String eventType,
      String argsHash) {
    return new AuditEvent(
        tenantId, channel, ActorKind.of(agent), actorHash, eventType, argsHash, 0, null, null);
  }
}
