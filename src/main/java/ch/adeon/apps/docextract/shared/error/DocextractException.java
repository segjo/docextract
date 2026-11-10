package ch.adeon.apps.docextract.shared.error;

/**
 * Base of all failures that are reported to a caller (REST or MCP). {@link #getMessage()} is for
 * logs; {@link #detail()} is what the caller sees and never carries PII (C-4): no attribute
 * values, no file names, no upstream response bodies.
 */
public abstract class DocextractException extends RuntimeException {

  private final ProblemCode code;
  private final String detail;
  private final String processId;

  protected DocextractException(
      ProblemCode code, String message, String detail, String processId, Throwable cause) {
    super(message, cause);
    this.code = code;
    this.detail = detail;
    this.processId = processId;
  }

  public ProblemCode code() {
    return code;
  }

  public String detail() {
    return detail;
  }

  /** The process this failure belongs to, {@code null} if there is none. */
  public String processId() {
    return processId;
  }
}
