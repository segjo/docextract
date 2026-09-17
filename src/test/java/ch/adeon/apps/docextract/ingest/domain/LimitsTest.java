package ch.adeon.apps.docextract.ingest.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LimitsTest {

  private final Limits limits = new Limits(10);

  @Test
  void rejects_empty_upload() {
    ValidationResult result =
        limits.validate(new DocumentUpload("a.pdf", MediaType.PDF, new byte[0]));

    assertThat(result.valid()).isFalse();
  }

  @Test
  void rejects_upload_exceeding_max_size() {
    ValidationResult result =
        limits.validate(new DocumentUpload("a.pdf", MediaType.PDF, new byte[11]));

    assertThat(result.valid()).isFalse();
  }

  @Test
  void accepts_upload_within_limit() {
    ValidationResult result =
        limits.validate(new DocumentUpload("a.pdf", MediaType.PDF, new byte[10]));

    assertThat(result.valid()).isTrue();
  }
}
