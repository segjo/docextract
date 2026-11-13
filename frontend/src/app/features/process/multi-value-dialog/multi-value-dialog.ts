import {
  afterNextRender,
  Component,
  computed,
  ElementRef,
  input,
  linkedSignal,
  output,
  viewChild,
} from '@angular/core';
import { DmsPropertyDefinition } from '../../../core/models/retrieval.model';
import { fromRows, toRows } from '../../../core/multi-value-rows';

/** Edits all multi-value properties of a document type as a table: columns = properties, rows = positions. */
@Component({
  selector: 'app-multi-value-dialog',
  templateUrl: './multi-value-dialog.html',
  styleUrl: './multi-value-dialog.scss',
})
export class MultiValueDialog {
  readonly definitions = input.required<DmsPropertyDefinition[]>();
  /** Current values per property id. */
  readonly values = input.required<Record<string, string[]>>();

  readonly saved = output<Record<string, string[]>>();
  readonly cancelled = output<void>();

  private readonly dialog = viewChild.required<ElementRef<HTMLElement>>('dialog');

  protected readonly rows = linkedSignal(() =>
    toRows(
      this.definitions().map((definition) => definition.id),
      this.values(),
    ),
  );

  /** The smallest `maxRowCount` of any column limits the table. */
  protected readonly canAddRow = computed(() => {
    const limits = this.definitions()
      .map((definition) => definition.maxRowCount)
      .filter((limit): limit is number => limit !== null && limit > 0);
    return limits.length === 0 || this.rows().length < Math.min(...limits);
  });

  constructor() {
    afterNextRender(() => this.dialog().nativeElement.focus());
  }

  protected options(definition: DmsPropertyDefinition, current: string): string[] {
    return current && !definition.valueList.includes(current)
      ? [...definition.valueList, current]
      : definition.valueList;
  }

  protected setCell(rowIndex: number, propertyId: string, value: string): void {
    this.rows.update((rows) =>
      rows.map((row, index) => (index === rowIndex ? { ...row, [propertyId]: value } : row)),
    );
  }

  protected addRow(): void {
    this.rows.update((rows) => [
      ...rows,
      Object.fromEntries(this.definitions().map((definition) => [definition.id, ''])),
    ]);
  }

  protected removeRow(rowIndex: number): void {
    this.rows.update((rows) => rows.filter((_, index) => index !== rowIndex));
  }

  protected save(): void {
    this.saved.emit(
      fromRows(
        this.definitions().map((definition) => definition.id),
        this.rows(),
      ),
    );
  }
}
