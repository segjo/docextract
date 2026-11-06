package ch.adeon.apps.docextract.process.adapter.in.sse;

import static ch.adeon.apps.docextract.shared.config.OpenApiConfig.UNAUTHORIZED_REF;

import ch.adeon.apps.docextract.process.application.ProcessEvent;
import ch.adeon.apps.docextract.process.application.ProcessEventListener;
import ch.adeon.apps.docextract.process.application.ProcessEventPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "process", description = "Fortschritt je Prozess (ADR-004)")
public class ProcessEventsController {

  private final ProcessEventPort processEventPort;

  public ProcessEventsController(ProcessEventPort processEventPort) {
    this.processEventPort = processEventPort;
  }

  @GetMapping(path = "/{processId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @Operation(
      operationId = "processEvents",
      summary = "Fortschritt als Server-Sent Events",
      description =
          "Event-Name = Schritt in Kleinbuchstaben (z. B. `extracted`), Daten = ProcessEvent."
              + " Beim (Re-)Connect wird die Historie aus PROCESS_STEP nachgeliefert. Payload"
              + " PII-frei (nur processId/step/status).")
  @ApiResponse(
      responseCode = "200",
      description = "Event-Stream (offen bis Client trennt).",
      content =
          @Content(
              mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
              schema = @Schema(implementation = ProcessEvent.class)))
  @ApiResponse(responseCode = "401", ref = UNAUTHORIZED_REF)
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
