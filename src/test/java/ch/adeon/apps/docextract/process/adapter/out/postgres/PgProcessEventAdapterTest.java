package ch.adeon.apps.docextract.process.adapter.out.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import ch.adeon.apps.docextract.process.application.ProcessEventListener;
import ch.adeon.apps.docextract.process.domain.ProcessEvent;
import ch.adeon.apps.docextract.process.domain.ProcessStep;
import ch.adeon.apps.docextract.process.domain.StepStatus;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
class PgProcessEventAdapterTest {

  @Container
  @SuppressWarnings("resource") // lifecycle managed by the @Testcontainers JUnit extension
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:17-alpine")
          .withDatabaseName("docextract")
          .withUsername("docextract")
          .withPassword("docextract-test");

  private static JdbcTemplate jdbcTemplate;
  private static PgProcessEventAdapter adapter;

  @BeforeAll
  static void migrateAndStart() throws InterruptedException {
    DataSource dataSource =
        DataSourceBuilder.create()
            .url(POSTGRES.getJdbcUrl())
            .username(POSTGRES.getUsername())
            .password(POSTGRES.getPassword())
            .driverClassName("org.postgresql.Driver")
            .build();
    Flyway.configure().dataSource(dataSource).load().migrate();
    jdbcTemplate = new JdbcTemplate(dataSource);
    adapter =
        new PgProcessEventAdapter(
            jdbcTemplate,
            new ObjectMapper(),
            POSTGRES.getJdbcUrl(),
            POSTGRES.getUsername(),
            POSTGRES.getPassword());
    adapter.start();
    // give the listener thread time to open its connection and issue LISTEN before publishing
    Thread.sleep(500);
  }

  @AfterAll
  static void stop() {
    adapter.stop();
  }

  @AfterEach
  void clearTable() {
    jdbcTemplate.update("DELETE FROM process_step");
  }

  @Test
  void live_subscriber_receives_a_published_event_via_notify() throws InterruptedException {
    BlockingQueue<ProcessEvent> received = new ArrayBlockingQueue<>(1);
    ProcessEventListener listener = received::offer;

    adapter.subscribe("process-live", listener);
    adapter.publish(
        new ProcessEvent("process-live", ProcessStep.BLOB_STORED, StepStatus.COMPLETED, "blob-1"));

    ProcessEvent event = received.poll(5, TimeUnit.SECONDS);
    assertThat(event)
        .isEqualTo(
            new ProcessEvent(
                "process-live", ProcessStep.BLOB_STORED, StepStatus.COMPLETED, "blob-1"));

    adapter.unsubscribe("process-live", listener);
  }

  @Test
  void new_subscriber_replays_already_published_history() {
    adapter.publish(
        new ProcessEvent("process-replay", ProcessStep.BLOB_STORED, StepStatus.COMPLETED, "b1"));
    adapter.publish(
        new ProcessEvent("process-replay", ProcessStep.DMS_UPLOADED, StepStatus.COMPLETED, null));

    List<ProcessEvent> replayed = new java.util.concurrent.CopyOnWriteArrayList<>();
    adapter.subscribe("process-replay", replayed::add);

    assertThat(replayed)
        .containsExactly(
            new ProcessEvent("process-replay", ProcessStep.BLOB_STORED, StepStatus.COMPLETED, "b1"),
            new ProcessEvent(
                "process-replay", ProcessStep.DMS_UPLOADED, StepStatus.COMPLETED, null));
  }

  @Test
  void unsubscribed_listener_no_longer_receives_events() throws InterruptedException {
    BlockingQueue<ProcessEvent> received = new ArrayBlockingQueue<>(1);
    ProcessEventListener listener = received::offer;
    adapter.subscribe("process-unsub", listener);
    adapter.unsubscribe("process-unsub", listener);

    adapter.publish(
        new ProcessEvent("process-unsub", ProcessStep.PREVIEW, StepStatus.STARTED, null));

    assertThat(received.poll(1, TimeUnit.SECONDS)).isNull();
  }
}
