package ch.adeon.apps.docextract.process.application;

import ch.adeon.apps.docextract.process.domain.ProcessEvent;

/**
 * Cross-module progress port (ADR-004): other modules publish step events directly through this
 * port, the same way {@code audit.application.AuditPort} is used across modules. The SSE adapter
 * subscribes/unsubscribes per {@code processId} to fan events out to connected clients.
 */
public interface ProcessEventPort {

  void publish(ProcessEvent event);

  void subscribe(String processId, ProcessEventListener listener);

  void unsubscribe(String processId, ProcessEventListener listener);
}
