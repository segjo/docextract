package ch.adeon.apps.docextract.shared.error;

/** The requested byte range starts beyond the end of the representation (RFC 9110 § 15.5.17). */
public class RangeNotSatisfiableException extends DocextractException {

  private final long totalSize;

  public RangeNotSatisfiableException(long totalSize) {
    super(
        ProblemCode.RANGE_NOT_SATISFIABLE,
        "range outside of a representation of " + totalSize + " bytes",
        "The requested range lies outside of the document",
        null,
        null);
    this.totalSize = totalSize;
  }

  public long totalSize() {
    return totalSize;
  }
}
