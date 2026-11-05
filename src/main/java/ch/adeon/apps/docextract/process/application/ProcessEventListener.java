package ch.adeon.apps.docextract.process.application;


/** Callback used by the SSE adapter to receive events for a subscribed {@code processId}. */
@FunctionalInterface
public interface ProcessEventListener {

  void onEvent(ProcessEvent event);
}
