import { Service } from '@angular/core';
import { Observable } from 'rxjs';
import { ProcessEvent, ProcessStep } from '../models/process.model';
import { API_BASE } from './api-base';
import { MOCK_API } from '../mock/mock-flag';
import { mockProcessEvents } from '../mock/mock-data';

const STEP_EVENT_NAMES: ProcessStep[] = [
  'BLOB_STORED',
  'DMS_UPLOADED',
  'PREVIEW',
  'TEXT_EXTRACTED',
  'RETRIEVED',
  'EXTRACTED',
];

const MOCK_EVENT_INTERVAL_MS = 400;

/** SSE progress stream for a single processId (ADR-004) — payload carries only processId/step, never PII. */
@Service()
export class ProcessEventsApi {
  subscribe(processId: string): Observable<ProcessEvent> {
    if (MOCK_API) {
      return this.subscribeMock(processId);
    }
    return new Observable<ProcessEvent>((subscriber) => {
      const source = new EventSource(
        `${API_BASE}/processes/${encodeURIComponent(processId)}/events`,
      );

      const onEvent = (message: MessageEvent<string>) => {
        subscriber.next(JSON.parse(message.data) as ProcessEvent);
      };
      for (const step of STEP_EVENT_NAMES) {
        source.addEventListener(step.toLowerCase(), onEvent);
      }
      source.onerror = () => {
        // A clean server-side emitter completion also raises `onerror` (EventSource auto-reconnects
        // otherwise); treat it as stream end rather than surfacing a user-facing error.
        subscriber.complete();
        source.close();
      };

      return () => source.close();
    });
  }

  /** No real backend to push SSE from in mock mode — replays a canned event sequence on a timer instead. */
  private subscribeMock(processId: string): Observable<ProcessEvent> {
    return new Observable<ProcessEvent>((subscriber) => {
      const events = mockProcessEvents(processId);
      let index = 0;
      const timer = setInterval(() => {
        subscriber.next(events[index]);
        index++;
        if (index >= events.length) {
          clearInterval(timer);
          subscriber.complete();
        }
      }, MOCK_EVENT_INTERVAL_MS);
      return () => clearInterval(timer);
    });
  }
}
