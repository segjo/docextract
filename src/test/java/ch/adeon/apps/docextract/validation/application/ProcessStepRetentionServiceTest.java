package ch.adeon.apps.docextract.validation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.adeon.apps.docextract.process.application.ProcessEventPort;
import ch.adeon.apps.docextract.validation.domain.Finalization;
import ch.adeon.apps.docextract.validation.domain.FinalizationStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ProcessStepRetentionServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");
  private static final Duration H2 = Duration.ofHours(2);

  private final ProcessEventPort processEventPort = mock(ProcessEventPort.class);
  private final FinalizationStatePort finalizationStatePort = mock(FinalizationStatePort.class);

  private ProcessStepRetentionService service(Duration stepRetention, Duration promotionLimit) {
    return new ProcessStepRetentionService(
        processEventPort,
        finalizationStatePort,
        Clock.fixed(NOW, ZoneOffset.UTC),
        stepRetention,
        H2,
        H2,
        H2,
        H2,
        promotionLimit);
  }

  @Test
  void purges_steps_idle_longer_than_the_retention_and_skips_index_pending_processes() {
    when(finalizationStatePort.findByStatus(FinalizationStatus.FINALIZED_INDEX_PENDING))
        .thenReturn(
            List.of(
                new Finalization(
                    "p-pending",
                    "tenant-a",
                    FinalizationStatus.FINALIZED_INDEX_PENDING,
                    "repo-1",
                    "doc-1")));
    when(processEventPort.deleteStepsOfIdleProcesses(any(), any())).thenReturn(3);

    int deleted = service(Duration.ofHours(48), Duration.ofHours(24)).purgeExpired();

    assertThat(deleted).isEqualTo(3);
    verify(processEventPort)
        .deleteStepsOfIdleProcesses(eq(NOW.minus(Duration.ofHours(48))), eq(Set.of("p-pending")));
  }

  @Test
  void accepts_a_retention_equal_to_the_longest_ttl() {
    assertThat(service(Duration.ofHours(24), Duration.ofHours(24))).isNotNull();
  }

  @Test
  void startup_aborts_when_the_retention_is_shorter_than_the_promotion_retry_limit() {
    assertThatThrownBy(() -> service(Duration.ofHours(23), Duration.ofHours(24)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("docextract.process.step-retention");
  }

  // blob-ttl, retrieval result-ttl, pending-embedding-ttl, extraction result-ttl
  @ParameterizedTest
  @CsvSource({"0", "1", "2", "3"})
  void startup_aborts_when_the_retention_is_shorter_than_any_data_ttl(int index) {
    Duration[] ttls = {H2, H2, H2, H2};
    ttls[index] = Duration.ofHours(72);

    assertThatThrownBy(() ->
            new ProcessStepRetentionService(
                processEventPort,
                finalizationStatePort,
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofHours(48),
                ttls[0],
                ttls[1],
                ttls[2],
                ttls[3],
                Duration.ofHours(24)))
        .isInstanceOf(IllegalStateException.class);
  }
}
