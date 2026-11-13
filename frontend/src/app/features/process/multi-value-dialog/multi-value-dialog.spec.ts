import { TestBed } from '@angular/core/testing';
import { axe } from 'vitest-axe';
import { DmsPropertyDefinition } from '../../../core/models/retrieval.model';
import { MultiValueDialog } from './multi-value-dialog';

function definition(partial: Partial<DmsPropertyDefinition>): DmsPropertyDefinition {
  return {
    id: 'p1',
    name: 'Position',
    dataType: 'ALPHANUMERIC',
    mandatory: false,
    hasValueList: false,
    isDynamicValueList: false,
    isHidden: false,
    isModifiable: true,
    isSystemProperty: false,
    isMultiAttribute: true,
    isDocumentTypeProperty: false,
    maxRowCount: null,
    maxLength: null,
    valueList: [],
    hasMoreValues: false,
    ...partial,
  };
}

describe('MultiValueDialog', () => {
  async function createComponent(
    definitions: DmsPropertyDefinition[],
    values: Record<string, string[]>,
  ) {
    await TestBed.configureTestingModule({ imports: [MultiValueDialog] }).compileComponents();
    const fixture = TestBed.createComponent(MultiValueDialog);
    fixture.componentRef.setInput('definitions', definitions);
    fixture.componentRef.setInput('values', values);
    fixture.detectChanges();
    return fixture;
  }

  const el = (fixture: { nativeElement: unknown }) => fixture.nativeElement as HTMLElement;

  it('renders properties as columns and numbered rows, with the matching input per cell', async () => {
    const fixture = await createComponent(
      [
        definition({ id: 'a', name: 'Artikel' }),
        definition({ id: 'b', name: 'Typ', hasValueList: true, valueList: ['X', 'Y'] }),
      ],
      { a: ['1', '2'], b: ['Y'] },
    );

    const headers = Array.from(el(fixture).querySelectorAll('thead th'), (th) =>
      th.textContent?.trim(),
    );
    expect(headers).toEqual(['Nr.', 'Artikel', 'Typ', 'Aktionen']);
    const rows = el(fixture).querySelectorAll('tbody tr');
    expect(rows.length).toBe(2);
    expect(rows[1].querySelector('th')?.textContent?.trim()).toBe('2');
    expect((rows[0].querySelector('input') as HTMLInputElement).value).toBe('1');
    expect((rows[0].querySelector('select') as HTMLSelectElement).value).toBe('Y');
    expect((rows[1].querySelector('select') as HTMLSelectElement).value).toBe('');
  });

  it('adds, edits and removes rows and emits the columns on save', async () => {
    const fixture = await createComponent([definition({ id: 'a' }), definition({ id: 'b' })], {
      a: ['1'],
      b: ['x'],
    });
    const saved: Record<string, string[]>[] = [];
    fixture.componentInstance.saved.subscribe((value) => saved.push(value));

    (el(fixture).querySelector('.row-add') as HTMLButtonElement).click();
    fixture.detectChanges();
    const inputs = el(fixture).querySelectorAll('tbody input');
    const cell = inputs[2] as HTMLInputElement; // row 2, column a
    cell.value = '2';
    cell.dispatchEvent(new Event('input'));
    (el(fixture).querySelector('.btn--primary') as HTMLButtonElement).click();

    expect(saved).toEqual([{ a: ['1', '2'], b: ['x'] }]);

    (el(fixture).querySelector('.row-remove') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(el(fixture).querySelectorAll('tbody tr').length).toBe(1);
  });

  it('limits the rows to the smallest maxRowCount', async () => {
    const fixture = await createComponent(
      [definition({ id: 'a', maxRowCount: 2 }), definition({ id: 'b', maxRowCount: 5 })],
      { a: ['1', '2'] },
    );
    expect((el(fixture).querySelector('.row-add') as HTMLButtonElement).disabled).toBe(true);
  });

  it('emits cancelled on Escape and on "Abbrechen"', async () => {
    const fixture = await createComponent([definition({ id: 'a' })], { a: ['1'] });
    const spy = vi.fn();
    fixture.componentInstance.cancelled.subscribe(spy);

    el(fixture)
      .querySelector('.dialog')!
      .dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    expect(spy).toHaveBeenCalledTimes(1);

    const cancel = Array.from(el(fixture).querySelectorAll('.btn')).find((b) =>
      b.textContent?.includes('Abbrechen'),
    ) as HTMLButtonElement;
    cancel.click();
    expect(spy).toHaveBeenCalledTimes(2);
  });

  it('has no accessibility violations (NfA-F1)', async () => {
    const fixture = await createComponent([definition({ id: 'a' })], { a: ['1'] });
    const violations = (await axe(el(fixture))).violations;
    expect(violations).toEqual([]);
  });
});
