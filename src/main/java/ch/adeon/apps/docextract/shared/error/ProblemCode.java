package ch.adeon.apps.docextract.shared.error;

/**
 * Stable error codes shared by REST (ProblemDetail {@code type}) and the MCP tool error format
 * (ARCHITECTURE.md § 8.1). Framework-free: the HTTP status is chosen by the REST adapter.
 */
public enum ProblemCode {
  INVALID_REQUEST("invalid-request", "Invalid request"),
  INVALID_ATTRIBUTES("invalid-attributes", "Invalid attributes"),
  UNAUTHORIZED("unauthorized", "Unauthorized"),
  FORBIDDEN("forbidden", "Forbidden"),
  NOT_FOUND("not-found", "Not found"),
  PROCESS_EXPIRED("process-expired", "Process expired"),
  CONFLICT_STATE("conflict-state", "Conflicting state"),
  LIMIT_EXCEEDED("limit-exceeded", "Limit exceeded"),
  RANGE_NOT_SATISFIABLE("range-not-satisfiable", "Range not satisfiable"),
  ADAPTER_UNAVAILABLE("adapter-unavailable", "Upstream service unavailable"),
  INTERNAL_ERROR("internal-error", "Internal error");

  public static final String TYPE_PREFIX = "urn:docextract:problem:";

  private final String slug;
  private final String title;

  ProblemCode(String slug, String title) {
    this.slug = slug;
    this.title = title;
  }

  /** The {@code code} of the MCP error format and the last segment of the type URI. */
  public String slug() {
    return slug;
  }

  public String title() {
    return title;
  }

  public String typeUri() {
    return TYPE_PREFIX + slug;
  }
}
