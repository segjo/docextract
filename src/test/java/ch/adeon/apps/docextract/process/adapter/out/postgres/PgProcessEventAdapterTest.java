package ch.adeon.apps.docextract.process.adapter.out.postgres;

import static org.assertj.core.api.Assertions.assertThat;

import ch.adeon.apps.docextract.process.application.ProcessEvent;
import ch.adeon.apps.docextract.process.application.ProcessEventListener;
import ch.adeon.apps.docextract.process.application.ProcessStep;
import ch.adeon.apps.docextract.process.application.StepStatus;
import java.time.Duration;
import java.time.Instant;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
class PgProcessEventAdapterTest {

  @Container
  @SuppressWarnings("resource") // lifecycle managed by the @Testcontainers JUnit extension
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(
              DockerImageName.parse("pgvector/pgvector:pg17-bookworm")
                  .asCompatibleSubstituteFor("postgres"))
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
    // V3__embedding.sql relies on the vector extension shipped by the pgvector/pgvector image
    // (docker-compose.yml provisions it the same way for local/prod runs).
    new JdbcTemplate(dataSource).execute("CREATE EXTENSION IF NOT EXISTS vector");
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

    adapter.subscribe("t", "process-live", listener);
    adapter.publish(
        "t",
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
        "t",
        new ProcessEvent("process-replay", ProcessStep.BLOB_STORED, StepStatus.COMPLETED, "b1"));
    adapter.publish(
        "t",
        new ProcessEvent("process-replay", ProcessStep.DMS_UPLOADED, StepStatus.COMPLETED, null));

    List<ProcessEvent> replayed = new java.util.concurrent.CopyOnWriteArrayList<>();
    adapter.subscribe("t", "process-replay", replayed::add);

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
    adapter.subscribe("t", "process-unsub", listener);
    adapter.unsubscribe("process-unsub", listener);

    adapter.publish(
        "t", new ProcessEvent("process-unsub", ProcessStep.PREVIEW, StepStatus.STARTED, null));

    assertThat(received.poll(1, TimeUnit.SECONDS)).isNull();
  }

  @Test
  void another_tenants_subscriber_gets_neither_history_nor_live_events()
      throws InterruptedException {
    adapter.publish(
        "tenant-a",
        new ProcessEvent("process-foreign", ProcessStep.BLOB_STORED, StepStatus.COMPLETED, "b1"));
    BlockingQueue<ProcessEvent> received = new ArrayBlockingQueue<>(5);

    adapter.subscribe("tenant-b", "process-foreign", received::offer);
    adapter.publish(
        "tenant-a",
        new ProcessEvent("process-foreign", ProcessStep.PREVIEW, StepStatus.STARTED, null));

    assertThat(received.poll(1, TimeUnit.SECONDS)).isNull();
  }

  @Test
  void owner_still_receives_events_while_a_foreign_subscriber_listens_on_the_same_process()
      throws InterruptedException {
    BlockingQueue<ProcessEvent> owner = new ArrayBlockingQueue<>(5);
    BlockingQueue<ProcessEvent> foreign = new ArrayBlockingQueue<>(5);
    adapter.subscribe("tenant-a", "process-shared-id", owner::offer);
    adapter.subscribe("tenant-b", "process-shared-id", foreign::offer);

    adapter.publish(
        "tenant-a",
        new ProcessEvent("process-shared-id", ProcessStep.PREVIEW, StepStatus.STARTED, null));

    assertThat(owner.poll(5, TimeUnit.SECONDS)).isNotNull();
    assertThat(foreign.poll(500, TimeUnit.MILLISECONDS)).isNull();
  }

  @Test
  void process_exists_for_the_tenant_that_published_a_step_only() {
    adapter.publish(
        "tenant-a",
        new ProcessEvent("process-exists", ProcessStep.BLOB_STORED, StepStatus.COMPLETED, "b1"));

    assertThat(adapter.exists("tenant-a", "process-exists")).isTrue();
    assertThat(adapter.exists("tenant-b", "process-exists")).isFalse();
    assertThat(adapter.exists("tenant-a", "process-unknown")).isFalse();
  }

  @Test
  void retention_deletes_idle_processes_only_and_keeps_the_excluded_ones() {
    publishStep("p-idle", "BLOB_STORED");
    publishStep("p-idle", "DMS_UPLOADED");
    publishStep("p-kept", "DMS_UPLOADED");
    publishStep("p-active", "BLOB_STORED");
    publishStep("p-active", "DMS_UPLOADED");
    // p-active: only the first step is old, the newest is recent -> the whole process stays
    jdbcTemplate.update(
        "UPDATE process_step SET created_at = now() - interval '3 days'"
            + " WHERE process_id = 'p-idle' OR process_id = 'p-kept'"
            + " OR (process_id = 'p-active' AND step = 'BLOB_STORED')");

    int deleted =
        adapter.deleteStepsOfIdleProcesses(
            Instant.now().minus(Duration.ofHours(48)), List.of("p-kept"));

    assertThat(deleted).isEqualTo(2);
    assertThat(adapter.exists("tenant-a", "p-idle")).isFalse();
    assertThat(adapter.exists("tenant-a", "p-kept")).isTrue();
    assertThat(adapter.exists("tenant-a", "p-active")).isTrue();
  }

  @Test
  void retention_query_is_backed_by_an_index_on_process_id_and_created_at() {
    String definition =
        jdbcTemplate.queryForObject(
            "SELECT indexdef FROM pg_indexes WHERE indexname = 'idx_process_step_process_created'",
            String.class);

    assertThat(definition).contains("process_step").contains("(process_id, created_at)");
  }

  @Test
  void retention_without_excluded_ids_works() {
    publishStep("p-old", "DMS_UPLOADED");
    jdbcTemplate.update("UPDATE process_step SET created_at = now() - interval '3 days'");

    assertThat(
            adapter.deleteStepsOfIdleProcesses(
                Instant.now().minus(Duration.ofHours(48)), List.of()))
        .isEqualTo(1);
  }

  private static void publishStep(String processId, String step) {
    adapter.publish(
        "tenant-a",
        new ProcessEvent(processId, ProcessStep.valueOf(step), StepStatus.COMPLETED, null));
  }
}
