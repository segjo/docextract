package ch.adeon.apps.docextract.process.application;

import ch.adeon.apps.docextract.process.domain.ProcessEvent;

/** Callback used by the SSE adapter to receive events for a subscribed {@code processId}. */
@FunctionalInterface
public interface ProcessEventListener {

  void onEvent(ProcessEvent event);
}
