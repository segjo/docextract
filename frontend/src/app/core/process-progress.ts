import { ProcessEvent, ProcessStep, PROCESS_STEP_ORDER } from './models/process.model';

export type DisplayStepStatus = 'pending' | 'running' | 'done' | 'error';

export const DISPLAY_STEP_STATUS_LABELS: Record<DisplayStepStatus, string> = {
  pending: 'wartet',
  running: 'laeuft ...',
  done: 'fertig',
  error: 'fehler',
};

/** Latest known status per step, derived from the ordered SSE/replay history for one processId. */
export function deriveStepStatuses(events: ProcessEvent[]): Record<ProcessStep, DisplayStepStatus> {
  const result = {} as Record<ProcessStep, DisplayStepStatus>;
  for (const step of PROCESS_STEP_ORDER) {
    result[step] = 'pending';
  }
  for (const event of events) {
    if (!(event.step in result)) {
      continue; // e.g. BLOB_STORED — internal, not shown in the protocol panel
    }
    result[event.step] =
      event.status === 'STARTED' ? 'running' : event.status === 'COMPLETED' ? 'done' : 'error';
  }
  return result;
}

/** Elapsed ms per step between its STARTED and COMPLETED/FAILED event, based on arrival times. */
export function deriveStepDurations(events: ProcessEvent[]): Partial<Record<ProcessStep, number>> {
  const started: Partial<Record<ProcessStep, number>> = {};
  const result: Partial<Record<ProcessStep, number>> = {};
  for (const event of events) {
    if (event.receivedAt === undefined) {
      continue;
    }
    if (event.status === 'STARTED') {
      started[event.step] = event.receivedAt;
    } else if (started[event.step] !== undefined) {
      result[event.step] = event.receivedAt - started[event.step]!;
    }
  }
  return result;
}

export function formatDuration(ms: number): string {
  return ms < 1000 ? `${Math.round(ms)} ms` : `${(ms / 1000).toFixed(1)} s`;
}

/** The PDF preview blob id, available once the PREVIEW step completes (native PDF or Gotenberg render). */
export function findPreviewBlobId(events: ProcessEvent[]): string | null {
  const previewCompleted = [...events]
    .reverse()
    .find((event) => event.step === 'PREVIEW' && event.status === 'COMPLETED');
  return previewCompleted?.blobId ?? null;
}

export function hasFailure(events: ProcessEvent[]): boolean {
  return events.some((event) => event.status === 'FAILED');
}

export function isExtractionComplete(events: ProcessEvent[]): boolean {
  return events.some((event) => event.step === 'EXTRACTED' && event.status === 'COMPLETED');
}

export function isRetrievalComplete(events: ProcessEvent[]): boolean {
  return events.some((event) => event.step === 'RETRIEVED' && event.status === 'COMPLETED');
}
