import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-status-bar',
  templateUrl: './status-bar.html',
  styleUrl: './status-bar.scss',
})
export class StatusBar {
  readonly attributeCount = input.required<number>();
  readonly criticalCount = input.required<number>();
  readonly toReviewCount = input.required<number>();
  readonly busy = input(false);
  /** True while a "Im DMS prüfen" request is in flight. */
  readonly checking = input(false);
  /** False while no document type is selected to check against. */
  readonly canCheck = input(true);

  readonly rejectRequested = output<void>();
  readonly checkRequested = output<void>();
  readonly confirmRequested = output<void>();
}
