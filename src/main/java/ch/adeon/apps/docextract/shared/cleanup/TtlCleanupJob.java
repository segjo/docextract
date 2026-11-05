package ch.adeon.apps.docextract.shared.cleanup;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic TTL cleanup over all {@link TtlPurger}s (ARCHITECTURE § 5.5). Purgers are idempotent, so
 * concurrent runs on several cluster nodes are harmless; one failing purger does not stop the
 * others.
 */
@Component
public class TtlCleanupJob {

  private static final Logger log = LoggerFactory.getLogger(TtlCleanupJob.class);

  private final List<TtlPurger> purgers;

  public TtlCleanupJob(List<TtlPurger> purgers) {
    this.purgers = purgers;
  }

  @Scheduled(
      fixedDelayString = "${docextract.cleanup.interval}",
      initialDelayString = "${docextract.cleanup.interval}")
  public void run() {
    for (TtlPurger purger : purgers) {
      try {
        int deleted = purger.purgeExpired();
        if (deleted > 0) {
          log.info("TTL cleanup {}: {} deleted", purger.getClass().getSimpleName(), deleted);
        }
      } catch (RuntimeException ex) {
        log.warn("TTL cleanup {} failed", purger.getClass().getSimpleName(), ex);
      }
    }
  }
}
