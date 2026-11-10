package ch.adeon.apps.docextract.validation.application;

import ch.adeon.apps.docextract.process.application.ProcessEventPort;
import ch.adeon.apps.docextract.shared.cleanup.TtlPurger;
import ch.adeon.apps.docextract.validation.domain.Finalization;
import ch.adeon.apps.docextract.validation.domain.FinalizationStatus;
import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Deletes the {@code process_step} rows of processes idle for longer than the retention. The steps
 * are the existence source of every process endpoint (404 vs. empty), so they must outlive all
 * result data; the constructor enforces that. A process in {@code FINALIZED_INDEX_PENDING} is never
 * purged (ADR-015).
 */
@Service
public class ProcessStepRetentionService implements TtlPurger {

  private final ProcessEventPort processEventPort;
  private final FinalizationStatePort finalizationStatePort;
  private final Clock clock;
  private final Duration stepRetention;

  public ProcessStepRetentionService(
      ProcessEventPort processEventPort,
      FinalizationStatePort finalizationStatePort,
      Clock clock,
      @Value("${docextract.process.step-retention:PT48H}") Duration stepRetention,
      @Value("${docextract.ingest.blob-ttl:PT2H}") Duration blobTtl,
      @Value("${docextract.retrieval.result-ttl:PT2H}") Duration retrievalResultTtl,
      @Value("${docextract.retrieval.pending-embedding-ttl:PT2H}") Duration pendingEmbeddingTtl,
      @Value("${docextract.extraction.result-ttl:PT2H}") Duration extractionResultTtl,
      @Value("${docextract.validation.promotion-retry-limit:PT24H}") Duration promotionRetryLimit) {
    Duration longest =
        Stream.of(
                blobTtl,
                retrievalResultTtl,
                pendingEmbeddingTtl,
                extractionResultTtl,
                promotionRetryLimit)
            .max(Duration::compareTo)
            .orElseThrow();
    if (stepRetention.compareTo(longest) < 0) {
      throw new IllegalStateException(
          "docextract.process.step-retention ("
              + stepRetention
              + ") must be at least as long as every result/blob/pending-embedding TTL and the"
              + " promotion-retry-limit ("
              + longest
              + ")");
    }
    this.processEventPort = processEventPort;
    this.finalizationStatePort = finalizationStatePort;
    this.clock = clock;
    this.stepRetention = stepRetention;
  }

  @Override
  public int purgeExpired() {
    // System-wide job over all tenants.
    Set<String> keep =
        finalizationStatePort.findByStatus(FinalizationStatus.FINALIZED_INDEX_PENDING).stream()
            .map(Finalization::processId)
            .collect(Collectors.toSet());
    return processEventPort.deleteStepsOfIdleProcesses(clock.instant().minus(stepRetention), keep);
  }
}
