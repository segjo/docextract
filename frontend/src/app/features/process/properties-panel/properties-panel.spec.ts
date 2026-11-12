import { TestBed } from '@angular/core/testing';
import { axe } from 'vitest-axe';
import { ExtractedAttribute } from '../../../core/models/extraction.model';
import {
  DmsDocumentMetadata,
  DmsDocumentType,
  DmsPropertyDefinition,
} from '../../../core/models/retrieval.model';
import { ValidationReport } from '../../../core/models/validation.model';
import { PropertiesPanel } from './properties-panel';

function propertyDefinition(partial: Partial<DmsPropertyDefinition>): DmsPropertyDefinition {
  return {
    id: 'p1',
    name: 'Rechnungsnummer',
    dataType: 'ALPHANUMERIC',
    mandatory: false,
    hasValueList: false,
    isDynamicValueList: false,
    isHidden: false,
    isModifiable: true,
    isSystemProperty: false,
    isMultiAttribute: false,
    isDocumentTypeProperty: false,
    maxRowCount: null,
    maxLength: null,
    valueList: [],
    hasMoreValues: false,
    ...partial,
  };
}

function documentType(properties: DmsPropertyDefinition[]): DmsDocumentType {
  return { id: 't1', name: 'Rechnung', properties };
}

function attribute(partial: Partial<ExtractedAttribute>): ExtractedAttribute {
  return {
    propertyId: 'p1',
    value: '12345',
    values: null,
    confidence: 1,
    sourceExcerpt: null,
    ...partial,
  };
}

describe('PropertiesPanel', () => {
  async function createComponent() {
    await TestBed.configureTestingModule({ imports: [PropertiesPanel] }).compileComponents();
    const fixture = TestBed.createComponent(PropertiesPanel);
    fixture.componentRef.setInput('extractedAttributes', []);
    fixture.componentRef.setInput('attributes', {});
    return fixture;
  }

  it('shows the "kein passender Dokumenttyp" banner when no document type is known', async () => {
    const fixture = await createComponent();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('.banner--warn')).toBeTruthy();
  });

  it('renders one field per extracted attribute, marking low-confidence ones as critical', async () => {
    const fixture = await createComponent();
    fixture.componentRef.setInput('documentType', documentType([propertyDefinition({ id: 'p1' })]));
    fixture.componentRef.setInput('extractedAttributes', [attribute({ confidence: 0.5 })]);
    fixture.componentRef.setInput('attributes', { p1: '12345' });
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.banner--warn')).toBeNull();
    expect(el.querySelectorAll('.field').length).toBe(1);
    expect(el.querySelector('.field--low')).toBeTruthy();
    expect(el.querySelector('.field__critical-badge')).toBeTruthy();
  });

  it('hides non-mandatory empty properties until "Leere Eigenschaften anzeigen" is checked', async () => {
    const fixture = await createComponent();
    fixture.componentRef.setInput(
      'documentType',
      documentType([
        propertyDefinition({ id: 'p1' }),
        propertyDefinition({ id: 'p2', mandatory: true }),
      ]),
    );
    fixture.componentRef.setInput('extractedAttributes', []);
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    // Only the mandatory empty property (p2) is shown by default.
    expect(el.querySelectorAll('.field').length).toBe(1);

    const checkbox = el.querySelector('input[type="checkbox"]') as HTMLInputElement;
    checkbox.checked = true;
    checkbox.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelectorAll('.field').length).toBe(2);
  });

  it('updates the attributes model when a text field changes', async () => {
    const fixture = await createComponent();
    fixture.componentRef.setInput('documentType', documentType([propertyDefinition({ id: 'p1' })]));
    fixture.componentRef.setInput('extractedAttributes', [attribute({})]);
    fixture.componentRef.setInput('attributes', { p1: '12345' });
    fixture.detectChanges();

    const input = (fixture.nativeElement as HTMLElement).querySelector(
      'input[type="text"]',
    ) as HTMLInputElement;
    input.value = '99999';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(fixture.componentInstance.attributes()).toEqual({ p1: '99999' });
  });

  it('preselects the extracted value in a value list, even if the list lacks it', async () => {
    const fixture = await createComponent();
    fixture.componentRef.setInput(
      'documentType',
      documentType([
        propertyDefinition({ id: 'p1', hasValueList: true, valueList: ['A', 'B'] }),
        propertyDefinition({ id: 'p2', hasValueList: true, valueList: ['A', 'B'] }),
      ]),
    );
    fixture.componentRef.setInput('extractedAttributes', [
      attribute({ propertyId: 'p1', value: 'B' }),
      attribute({ propertyId: 'p2', value: 'C' }),
    ]);
    fixture.componentRef.setInput('attributes', { p1: 'B', p2: 'C' });
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect((el.querySelector('#field-p1') as HTMLSelectElement).value).toBe('B');
    expect((el.querySelector('#field-p2') as HTMLSelectElement).value).toBe('C');
  });

  it('shows a multi-value property as a summary and edits it in the table dialog', async () => {
    const fixture = await createComponent();
    fixture.componentRef.setInput(
      'documentType',
      documentType([
        propertyDefinition({ id: 'p1', isMultiAttribute: true }),
        propertyDefinition({
          id: 'p2',
          isMultiAttribute: true,
          hasValueList: true,
          valueList: ['A', 'B', 'C'],
        }),
      ]),
    );
    fixture.componentRef.setInput('extractedAttributes', [
      attribute({ propertyId: 'p1', value: null, values: ['x', 'y'] }),
    ]);
    fixture.componentRef.setInput('attributes', { p1: ['x', 'y'] });
    fixture.componentRef.setInput('documentTypeCandidates', []);
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.field__multi')?.textContent).toContain('2 Werte');
    expect(el.querySelector('app-multi-value-dialog')).toBeNull();

    (el.querySelector('.field__multi-btn') as HTMLButtonElement).click();
    fixture.detectChanges();

    const dialog = el.querySelector('app-multi-value-dialog') as HTMLElement;
    expect(dialog.querySelectorAll('tbody tr').length).toBe(2);
    expect(dialog.querySelectorAll('thead th').length).toBe(4);

    const input = dialog.querySelector('input[type="text"]') as HTMLInputElement;
    input.value = 'z';
    input.dispatchEvent(new Event('input'));
    const select = dialog.querySelector('select') as HTMLSelectElement;
    select.value = 'B';
    select.dispatchEvent(new Event('change'));
    (dialog.querySelector('.btn--primary') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(fixture.componentInstance.attributes()).toEqual({ p1: ['z', 'y'], p2: ['B'] });
    expect(el.querySelector('app-multi-value-dialog')).toBeNull();
  });

  it('opens a confirmation dialog on document-type change and emits documentTypeChange on confirm', async () => {
    const candidates: DmsDocumentMetadata[] = [
      { documentType: documentType([]), contentLanguage: 'de', properties: [] },
      {
        documentType: { id: 't2', name: 'Lieferschein', properties: [] },
        contentLanguage: 'de',
        properties: [],
      },
    ];
    const fixture = await createComponent();
    fixture.componentRef.setInput('documentTypeCandidates', candidates);
    fixture.componentRef.setInput('selectedDocumentTypeId', 't1');
    fixture.detectChanges();

    const emitted: { documentTypeId: string; reExtract: boolean }[] = [];
    fixture.componentInstance.documentTypeChange.subscribe((change) => emitted.push(change));

    const select = (fixture.nativeElement as HTMLElement).querySelector(
      'select#document-type-select',
    ) as HTMLSelectElement;
    select.value = 't2';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('.dialog')).toBeTruthy();

    const confirmReExtractButton = (fixture.nativeElement as HTMLElement).querySelector(
      '.dialog__btn--primary',
    ) as HTMLButtonElement;
    confirmReExtractButton.click();
    fixture.detectChanges();

    expect(emitted).toEqual([{ documentTypeId: 't2', reExtract: true }]);
    expect(fixture.componentInstance.selectedDocumentTypeId()).toBe('t2');
    expect((fixture.nativeElement as HTMLElement).querySelector('.dialog')).toBeNull();
  });

  it('closes the dialog without emitting when "Abbrechen" is clicked', async () => {
    const candidates: DmsDocumentMetadata[] = [
      { documentType: documentType([]), contentLanguage: 'de', properties: [] },
      {
        documentType: { id: 't2', name: 'Lieferschein', properties: [] },
        contentLanguage: 'de',
        properties: [],
      },
    ];
    const fixture = await createComponent();
    fixture.componentRef.setInput('documentTypeCandidates', candidates);
    fixture.componentRef.setInput('selectedDocumentTypeId', 't1');
    fixture.detectChanges();

    const emitted: unknown[] = [];
    fixture.componentInstance.documentTypeChange.subscribe((change) => emitted.push(change));

    const select = (fixture.nativeElement as HTMLElement).querySelector(
      'select#document-type-select',
    ) as HTMLSelectElement;
    select.value = 't2';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    const cancelButton = (fixture.nativeElement as HTMLElement).querySelector(
      '.dialog__btn:not(.dialog__btn--primary)',
    ) as HTMLButtonElement;
    cancelButton.click();
    fixture.detectChanges();

    expect(emitted).toEqual([]);
    expect(fixture.componentInstance.selectedDocumentTypeId()).toBe('t1');
    expect((fixture.nativeElement as HTMLElement).querySelector('.dialog')).toBeNull();
  });

  describe('DMS check (ADR-016)', () => {
    async function checkedComponent(
      attributes: ExtractedAttribute[],
      report: ValidationReport | null,
      definitions = [
        propertyDefinition({ id: 'p1' }),
        propertyDefinition({ id: 'p2', name: 'Datum' }),
      ],
    ) {
      const fixture = await createComponent();
      fixture.componentRef.setInput('documentType', documentType(definitions));
      fixture.componentRef.setInput('selectedDocumentTypeId', 't1');
      fixture.componentRef.setInput('extractedAttributes', attributes);
      fixture.componentRef.setInput('attributes', { p1: '12345', p2: '2026-11-11' });
      fixture.componentRef.setInput('checkReport', report);
      fixture.detectChanges();
      return fixture;
    }

    const live = (fixture: { nativeElement: unknown }) =>
      (fixture.nativeElement as HTMLElement).querySelector(
        '.properties__check-result',
      ) as HTMLElement;

    it('announces the checking state in the live region', async () => {
      const fixture = await checkedComponent([attribute({})], null);

      fixture.componentRef.setInput('checking', true);
      fixture.detectChanges();

      expect(live(fixture).textContent).toContain('Prüfe im DMS');
    });

    it('announces the result in a polite live region that is always in the DOM', async () => {
      const fixture = await checkedComponent([attribute({})], null);
      const region = live(fixture);
      expect(region.getAttribute('role')).toBe('status');
      expect(region.getAttribute('aria-live')).toBe('polite');
      expect(region.textContent?.trim()).toBe('');

      fixture.componentRef.setInput('checkReport', { valid: true, attributes: [], error: null });
      fixture.detectChanges();

      expect(live(fixture)).toBe(region);
      expect(region.textContent).toContain('Im DMS geprüft: gültig.');
    });

    it('shows an error with a property id at its field, linked to the input', async () => {
      const fixture = await checkedComponent([attribute({}), attribute({ propertyId: 'p2' })], {
        valid: false,
        attributes: null,
        error: { propertyId: 'p2', dmsCode: '0010019', message: 'Pflichtfeld fehlt' },
      });

      const el = fixture.nativeElement as HTMLElement;
      const error = el.querySelector('#field-p2-error') as HTMLElement;
      expect(error.textContent).toContain('Pflichtfeld fehlt');
      const input = el.querySelector('#field-p2') as HTMLInputElement;
      expect(input.getAttribute('aria-invalid')).toBe('true');
      expect(input.getAttribute('aria-describedby')).toBe('field-p2-error');
      expect(el.querySelector('#field-p1')?.getAttribute('aria-invalid')).toBeNull();
      expect(live(fixture).textContent).toContain('Fehler bei „Datum“');
    });

    it('shows the field of an error even if it is an empty, optional property', async () => {
      const fixture = await checkedComponent([attribute({})], {
        valid: false,
        attributes: null,
        error: { propertyId: 'p2', dmsCode: null, message: 'Wert fehlt' },
      });

      expect((fixture.nativeElement as HTMLElement).querySelector('#field-p2-error')).toBeTruthy();
    });

    it('shows an error without a property id in general, not at a field', async () => {
      const fixture = await checkedComponent([attribute({})], {
        valid: false,
        attributes: null,
        error: { propertyId: null, dmsCode: '0000042', message: 'Allgemeiner DMS-Fehler' },
      });

      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector('.field__error')).toBeNull();
      expect(live(fixture).textContent).toContain('Allgemeiner DMS-Fehler');
      expect(live(fixture).classList).toContain('properties__check-result--invalid');
    });

    it('marks values set or adjusted by the DMS and fields corrected after a DMS error', async () => {
      const fixture = await checkedComponent(
        [
          attribute({ propertyId: 'p1', changedByDms: true }),
          attribute({
            propertyId: 'p2',
            value: 'Hook',
            confidence: null,
            origin: 'DMS',
            changedByDms: true,
          }),
          attribute({ propertyId: 'p3', correctedAfterValidation: true, confidence: 0.5 }),
        ],
        null,
        [
          propertyDefinition({ id: 'p1' }),
          propertyDefinition({ id: 'p2', name: 'Status' }),
          propertyDefinition({ id: 'p3', name: 'Lieferant' }),
        ],
      );

      const fields = (fixture.nativeElement as HTMLElement).querySelectorAll('.field');
      expect(fields[0].textContent).toContain('vom DMS angepasst');
      expect(fields[1].textContent).toContain('vom DMS gesetzt');
      expect(fields[2].textContent).toContain('nach DMS-Fehler korrigiert');
      expect(fields[1].classList).toContain('field--dms');
    });

    it('shows no confidence and no "kritisch" for a value the DMS set', async () => {
      const fixture = await checkedComponent(
        [attribute({ propertyId: 'p1', confidence: null, origin: 'DMS', changedByDms: true })],
        null,
      );

      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector('.field__confidence')).toBeNull();
      expect(el.querySelector('.field__critical-badge')).toBeNull();
    });

    it('has no serious accessibility violations with a check result and a field error', async () => {
      const fixture = await checkedComponent([attribute({}), attribute({ propertyId: 'p2' })], {
        valid: false,
        attributes: null,
        error: { propertyId: 'p2', dmsCode: '1', message: 'Pflichtfeld fehlt' },
      });

      const results = await axe(fixture.nativeElement as HTMLElement, {
        resultTypes: ['violations'],
      });
      expect(
        results.violations.filter((v) => ['critical', 'serious'].includes(v.impact ?? '')),
      ).toEqual([]);
    });
  });
});
