package ch.adeon.apps.docextract.agentgateway.application;

/** Controlled failure of an agent tool call; the message never carries PII (C-4). */
public class AgentToolException extends RuntimeException {

  public enum Code {
    INVALID_ARGUMENT,
    NOT_FOUND
  }

  private final Code code;

  public AgentToolException(Code code, String message) {
    super(message);
    this.code = code;
  }

  public Code code() {
    return code;
  }
}
