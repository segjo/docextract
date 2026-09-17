package ch.adeon.apps.docextract.process.adapter.out.postgres;

import ch.adeon.apps.docextract.process.application.ProcessEventListener;
import ch.adeon.apps.docextract.process.application.ProcessEventPort;
import ch.adeon.apps.docextract.process.domain.ProcessEvent;
import ch.adeon.apps.docextract.process.domain.ProcessStep;
import ch.adeon.apps.docextract.process.domain.StepStatus;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Cluster-wide fan-out for {@link ProcessEvent}s via Postgres {@code LISTEN/NOTIFY} (ADR-004):
 * every instance publishes to the durable, PII-free {@code process_step} table and broadcasts on
 * the {@value #CHANNEL} channel; every instance also runs a dedicated listener connection so
 * clients connected to any node in the cluster receive events published on any other node. New
 * subscribers first replay the durable history for their {@code processId} (reconnect catch-up),
 * then receive live events forwarded from the notify channel.
 */
@Component
public class PgProcessEventAdapter implements ProcessEventPort, SmartLifecycle {

  static final String CHANNEL = "process_step_events";

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final String jdbcUrl;
  private final String username;
  private final String password;
  private final ExecutorService listenerExecutor =
      Executors.newSingleThreadExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "process-event-listener");
            thread.setDaemon(true);
            return thread;
          });

  private final Map<String, List<Subscription>> subscriptionsByProcessId =
      new ConcurrentHashMap<>();

  private volatile boolean running;
  private final AtomicReference<Connection> listenConnection = new AtomicReference<>();

  public PgProcessEventAdapter(
      JdbcTemplate jdbcTemplate,
      ObjectMapper objectMapper,
      @Value("${spring.datasource.url}") String jdbcUrl,
      @Value("${spring.datasource.username}") String username,
      @Value("${spring.datasource.password}") String password) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.jdbcUrl = jdbcUrl;
    this.username = username;
    this.password = password;
  }

  @Override
  public void publish(ProcessEvent event) {
    Long id =
        jdbcTemplate.queryForObject(
            "INSERT INTO process_step (process_id, step, status, blob_id) VALUES (?, ?, ?, ?)"
                + " RETURNING id",
            Long.class,
            event.processId(),
            event.step().name(),
            event.status().name(),
            event.blobId());
    if (id == null) {
      throw new IllegalStateException("process_step insert did not return an id");
    }
    jdbcTemplate.query(
        "SELECT pg_notify(?, ?)",
        (rs, rowNum) -> null,
        CHANNEL,
        serialize(new NotifyPayload(id, event)));
  }

  @Override
  public void subscribe(String processId, ProcessEventListener listener) {
    Subscription subscription = new Subscription(listener);
    subscriptionsByProcessId
        .computeIfAbsent(processId, id -> new CopyOnWriteArrayList<>())
        .add(subscription);
    // Replay already-published steps first: preview generation may finish (native PDFs need no
    // rendering) before a client's SSE connection is even opened. The watermark on the
    // subscription prevents a racing live NOTIFY for one of these same rows from being delivered
    // twice.
    replayHistory(processId, subscription);
  }

  @Override
  public void unsubscribe(String processId, ProcessEventListener listener) {
    List<Subscription> subscriptions = subscriptionsByProcessId.get(processId);
    if (subscriptions != null) {
      subscriptions.removeIf(subscription -> subscription.listener == listener);
    }
  }

  private void replayHistory(String processId, Subscription subscription) {
    List<Object[]> rows =
        jdbcTemplate.query(
            "SELECT id, step, status, blob_id FROM process_step WHERE process_id = ? ORDER BY id",
            (rs, rowNum) ->
                new Object[] {
                  rs.getLong("id"),
                  new ProcessEvent(
                      processId,
                      ProcessStep.valueOf(rs.getString("step")),
                      StepStatus.valueOf(rs.getString("status")),
                      rs.getString("blob_id"))
                },
            processId);
    for (Object[] row : rows) {
      subscription.deliverIfNewer((long) row[0], (ProcessEvent) row[1]);
    }
  }

  private String serialize(NotifyPayload payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (Exception ex) {
      throw new IllegalStateException("failed to serialize process event for NOTIFY", ex);
    }
  }

  private void dispatch(String payload) {
    NotifyPayload notifyPayload;
    try {
      notifyPayload = objectMapper.readValue(payload, NotifyPayload.class);
    } catch (Exception ex) {
      return; // malformed/foreign payload on the shared channel; ignore
    }
    List<Subscription> subscriptions =
        subscriptionsByProcessId.get(notifyPayload.event().processId());
    if (subscriptions != null) {
      subscriptions.forEach(
          subscription -> subscription.deliverIfNewer(notifyPayload.id(), notifyPayload.event()));
    }
  }

  /** NOTIFY payload: carries the durable row id so subscribers can dedupe against replay. */
  private record NotifyPayload(long id, ProcessEvent event) {}

  /** Tracks the highest {@code process_step.id} already delivered to a listener (dedupe/replay). */
  private static final class Subscription {
    private final ProcessEventListener listener;
    private long lastDeliveredId;

    Subscription(ProcessEventListener listener) {
      this.listener = listener;
    }

    synchronized void deliverIfNewer(long id, ProcessEvent event) {
      if (id > lastDeliveredId) {
        lastDeliveredId = id;
        listener.onEvent(event);
      }
    }
  }

  @Override
  public void start() {
    running = true;
    listenerExecutor.submit(this::listenLoop);
  }

  private void listenLoop() {
    while (running) {
      try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
        listenConnection.set(connection);
        try (Statement statement = connection.createStatement()) {
          statement.execute("LISTEN " + CHANNEL);
        }
        PGConnection pgConnection = connection.unwrap(PGConnection.class);
        while (running) {
          // Blocks until a notification arrives or the connection is closed by stop().
          PGNotification[] notifications = pgConnection.getNotifications(0);
          if (notifications != null) {
            for (PGNotification notification : notifications) {
              dispatch(notification.getParameter());
            }
          }
        }
      } catch (SQLException ex) {
        if (running) {
          sleepBeforeReconnect();
        }
      } finally {
        listenConnection.set(null);
      }
    }
  }

  private void sleepBeforeReconnect() {
    try {
      Thread.sleep(2000);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void stop() {
    running = false;
    Connection connection = listenConnection.get();
    if (connection != null) {
      try {
        connection.close();
      } catch (SQLException ignored) {
        // closing to unblock the listener thread; nothing to act on
      }
    }
    listenerExecutor.shutdownNow();
  }

  @Override
  public boolean isRunning() {
    return running;
  }
}
