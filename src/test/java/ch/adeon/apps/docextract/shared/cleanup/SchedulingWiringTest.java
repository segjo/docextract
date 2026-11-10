package ch.adeon.apps.docextract.shared.cleanup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ch.adeon.apps.docextract.shared.config.SchedulingConfig;
import ch.adeon.apps.docextract.validation.adapter.in.scheduler.FinalizationRecoveryScheduler;
import ch.adeon.apps.docextract.validation.application.RetryCorpusPromotion;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

/** Boot-free check that both jobs are registered and their ISO-8601 intervals parse. */
class SchedulingWiringTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withPropertyValues(
              "docextract.cleanup.interval=PT5M", "docextract.validation.recovery-interval=PT5M")
          .withUserConfiguration(
              SchedulingConfig.class, TtlCleanupJob.class, FinalizationRecoveryScheduler.class)
          .withBean(RetryCorpusPromotion.class, () -> mock(RetryCorpusPromotion.class))
          .withBean(TtlPurger.class, () -> mock(TtlPurger.class));

  @Test
  void registers_ttl_cleanup_and_promotion_retry_jobs() {
    runner.run(context -> {
      assertThat(context).hasNotFailed();
      assertThat(context.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks())
          .hasSize(2);
    });
  }

  @Test
  void fails_fast_without_configured_interval() {
    new ApplicationContextRunner()
        .withUserConfiguration(SchedulingConfig.class, TtlCleanupJob.class)
        .withBean(TtlPurger.class, () -> mock(TtlPurger.class))
        .run(context -> assertThat(context).hasFailed());
  }
}
