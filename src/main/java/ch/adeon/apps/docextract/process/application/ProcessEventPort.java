package ch.adeon.apps.docextract.process.application;

import java.time.Instant;
import java.util.Collection;

/**
 * Cross-module progress port (ADR-004): other modules publish step events directly through this
 * port, the same way {@code audit.application.AuditPort} is used across modules. The SSE adapter
 * subscribes/unsubscribes per {@code processId} to fan events out to connected clients.
 */
public interface ProcessEventPort {

  void publish(String tenantId, ProcessEvent event);

  /**
   * Whether the tenant has published any step for the process, i.e. whether the process exists for
   * it. Steps are deleted only by the retention job, which keeps them longer than any result data.
   */
  boolean exists(String tenantId, String processId);

  /**
   * System-wide retention: deletes all steps of every process whose newest step is older than
   * {@code idleBefore}, except the given process ids. Returns the number of deleted steps.
   */
  int deleteStepsOfIdleProcesses(Instant idleBefore, Collection<String> keepProcessIds);

  /** Delivers only events (history and live) published for the same tenant. */
  void subscribe(String tenantId, String processId, ProcessEventListener listener);

  void unsubscribe(String processId, ProcessEventListener listener);
}
