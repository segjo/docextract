package ch.adeon.apps.docextract.ingest.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import ch.adeon.apps.docextract.ingest.port.PageRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PreviewControllerRangeTest {

  @Test
  void single_range_is_parsed() {
    assertThat(PreviewController.parseRange("bytes=5-9")).isEqualTo(new PageRange(5, 9));
    assertThat(PreviewController.parseRange("BYTES=5-"))
        .isEqualTo(new PageRange(5, Long.MAX_VALUE));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"", "garbage", "bytes=", "bytes=a-b", "bytes=9-5", "bytes=-500", "bytes=0-1,3-4"})
  void syntactically_invalid_or_unsupported_ranges_are_ignored(String header) {
    assertThat(PreviewController.parseRange(header)).isNull();
  }

  @Test
  void absent_header_is_no_range() {
    assertThat(PreviewController.parseRange(null)).isNull();
  }
}
