import { TestBed } from '@angular/core/testing';
import { ExtractedAttribute } from '../../../core/models/extraction.model';
import {
  DmsDocumentMetadata,
  DmsDocumentType,
  DmsPropertyDefinition,
} from '../../../core/models/retrieval.model';
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
});
