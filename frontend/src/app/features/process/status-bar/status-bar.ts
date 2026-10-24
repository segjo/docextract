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

  readonly rejectRequested = output<void>();
  readonly confirmRequested = output<void>();
}
