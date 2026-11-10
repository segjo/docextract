package ch.adeon.apps.docextract.shared.error;

/** A hard limit (T-4) was exceeded; the {@link Limit} decides the HTTP status. */
public class LimitExceededException extends DocextractException {

  public enum Limit {
    FILE_SIZE,
    PAGE_COUNT
  }

  private final Limit limit;

  public LimitExceededException(Limit limit, String message) {
    super(ProblemCode.LIMIT_EXCEEDED, message, message, null, null);
    this.limit = limit;
  }

  public Limit limit() {
    return limit;
  }
}
