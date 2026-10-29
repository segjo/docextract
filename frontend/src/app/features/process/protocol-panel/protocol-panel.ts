import { Component, computed, input } from '@angular/core';
import {
  ProcessEvent,
  PROCESS_STEP_LABELS,
  PROCESS_STEP_ORDER,
} from '../../../core/models/process.model';
import {
  deriveStepDurations,
  deriveStepStatuses,
  DISPLAY_STEP_STATUS_LABELS,
  formatDuration,
} from '../../../core/process-progress';

@Component({
  selector: 'app-protocol-panel',
  templateUrl: './protocol-panel.html',
  styleUrl: './protocol-panel.scss',
})
export class ProtocolPanel {
  readonly processId = input.required<string>();
  readonly events = input.required<ProcessEvent[]>();

  protected readonly steps = PROCESS_STEP_ORDER;
  protected readonly labels = PROCESS_STEP_LABELS;
  protected readonly statusLabels = DISPLAY_STEP_STATUS_LABELS;
  protected readonly statuses = computed(() => deriveStepStatuses(this.events()));
  protected readonly durations = computed(() => deriveStepDurations(this.events()));
  protected readonly formatDuration = formatDuration;
}
