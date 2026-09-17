package ch.adeon.apps.docextract.ingest.domain;

/** Hard upload limits (T-4 DoS mitigation). Kept framework-free per the hexagonal rules. */
public record Limits(long maxFileSizeBytes) {

  public ValidationResult validate(DocumentUpload upload) {
    if (upload.content() == null || upload.content().length == 0) {
      return ValidationResult.rejected("File must not be empty");
    }
    if (upload.sizeInBytes() > maxFileSizeBytes) {
      return ValidationResult.rejected(
          "File size %d bytes exceeds the maximum allowed size of %d bytes"
              .formatted(upload.sizeInBytes(), maxFileSizeBytes));
    }
    return ValidationResult.ok();
  }
}
