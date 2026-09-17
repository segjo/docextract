package ch.adeon.apps.docextract.process.adapter.in.sse;

import ch.adeon.apps.docextract.process.application.ProcessEventListener;
import ch.adeon.apps.docextract.process.application.ProcessEventPort;
import java.io.IOException;
import java.util.Locale;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * {@code GET /processes/{processId}/events} — SSE progress stream for a single process (ADR-004).
 */
@RestController
@RequestMapping("/api/v1/processes")
public class ProcessEventsController {

  private final ProcessEventPort processEventPort;

  public ProcessEventsController(ProcessEventPort processEventPort) {
    this.processEventPort = processEventPort;
  }

  @GetMapping(path = "/{processId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribe(@PathVariable String processId) {
    SseEmitter emitter = new SseEmitter(0L);
    ProcessEventListener listener =
        event -> {
          try {
            emitter.send(
                SseEmitter.event().name(event.step().name().toLowerCase(Locale.ROOT)).data(event));
          } catch (IOException ex) {
            emitter.completeWithError(ex);
          }
        };
    processEventPort.subscribe(processId, listener);
    Runnable unsubscribe = () -> processEventPort.unsubscribe(processId, listener);
    emitter.onCompletion(unsubscribe);
    emitter.onTimeout(unsubscribe);
    emitter.onError(ex -> unsubscribe.run());
    return emitter;
  }
}
