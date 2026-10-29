import { TestBed } from '@angular/core/testing';
import { RetrievalResult } from '../../../core/models/retrieval.model';
import { SimilarPanel } from './similar-panel';

describe('SimilarPanel', () => {
  async function createComponent(results: RetrievalResult[]) {
    await TestBed.configureTestingModule({ imports: [SimilarPanel] }).compileComponents();
    const fixture = TestBed.createComponent(SimilarPanel);
    fixture.componentRef.setInput('results', results);
    fixture.detectChanges();
    return fixture;
  }

  it('shows the empty-state message when there are no results', async () => {
    const fixture = await createComponent([]);
    expect((fixture.nativeElement as HTMLElement).querySelector('.similar__empty')).toBeTruthy();
  });

  it('renders one row per result with the DMS id and score percentage', async () => {
    const fixture = await createComponent([
      { repositoryId: 'r1', documentId: 'd1', score: 0.873, properties: null },
    ]);
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.similar__empty')).toBeNull();
    expect(el.querySelector('.similar__meta')!.textContent).toContain('d1');
    expect(el.querySelector('.similar__score')!.textContent).toContain('87');
  });

  it('shows the document type name parsed from the properties JSON', async () => {
    const properties = JSON.stringify({
      documentType: { id: 't1', name: 'Rechnung', properties: [] },
      contentLanguage: 'de',
      properties: [],
    });
    const fixture = await createComponent([
      { repositoryId: 'r1', documentId: 'd1', score: 1, properties },
    ]);
    expect(
      (fixture.nativeElement as HTMLElement).querySelector('.similar__name')!.textContent,
    ).toContain('Rechnung');
  });

  it('lists non-empty properties by name instead of raw JSON', async () => {
    const properties = JSON.stringify({
      documentType: {
        id: 't1',
        name: 'Rechnung',
        properties: [
          { id: 'property_owner', name: 'Im Besitz von' },
          { id: 'property_remark', name: 'Bemerkung' },
        ],
      },
      properties: [
        { id: 'property_owner', value: 'Jonas Segessemann', values: null },
        { id: 'property_remark', value: '', values: null },
        { id: 'unknown_id', value: null, values: ['a', 'b'] },
      ],
    });
    const fixture = await createComponent([
      { repositoryId: 'r1', documentId: 'd1', score: 1, properties },
    ]);
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.similar__json')).toBeNull();
    expect(Array.from(el.querySelectorAll('dt')).map((e) => e.textContent)).toEqual([
      'Im Besitz von',
      'unknown_id',
    ]);
    expect(Array.from(el.querySelectorAll('dd')).map((e) => e.textContent)).toEqual([
      'Jonas Segessemann',
      'a, b',
    ]);
  });

  it('shows property_caption as the row title when present', async () => {
    const properties = JSON.stringify({
      documentType: { id: 't1', name: 'Rechnung', properties: [] },
      properties: [{ id: 'property_caption', value: 'GitHub, Inc. - 28.10.2024', values: null }],
    });
    const fixture = await createComponent([
      { repositoryId: 'r1', documentId: 'd1', score: 1, properties },
    ]);
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.similar__name')!.textContent).toContain('GitHub, Inc. - 28.10.2024');
    expect(el.querySelector('.similar__type')!.textContent).toContain('Rechnung');
  });

  it('falls back to a generic label when properties is null or not parseable JSON', async () => {
    const fixture = await createComponent([
      { repositoryId: 'r1', documentId: 'd1', score: 1, properties: null },
      { repositoryId: 'r1', documentId: 'd2', score: 1, properties: 'not-json' },
    ]);
    const names = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('.similar__name'),
    ).map((el) => el.textContent);
    expect(names).toEqual(['Dokument', 'Dokument']);
  });
});
