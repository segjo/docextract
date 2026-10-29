import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { ExtractionApi } from '../../core/api/extraction-api';
import { ProcessEventsApi } from '../../core/api/process-events-api';
import { RetrievalApi } from '../../core/api/retrieval-api';
import { ValidationApi } from '../../core/api/validation-api';
import { ExtractionResult } from '../../core/models/extraction.model';
import { ProcessEvent } from '../../core/models/process.model';
import { ProcessPage } from './process-page';

function completedEvent(step: ProcessEvent['step']): ProcessEvent {
  return { processId: 'p-1', step, status: 'COMPLETED', blobId: null };
}

describe('ProcessPage', () => {
  let events$: Subject<ProcessEvent>;
  let extractionApi: Pick<ExtractionApi, 'extractionResult' | 'retryExtraction'>;
  let retrievalApi: Pick<
    RetrievalApi,
    'similarDocuments' | 'documentTypeCandidates' | 'objectDefinitions'
  >;
  let validationApi: Pick<ValidationApi, 'confirm' | 'reject'>;
  const extractionResult: ExtractionResult = {
    documentTypeId: 't1',
    attributes: [
      { propertyId: 'p1', value: 'v1', values: null, confidence: 1, sourceExcerpt: null },
    ],
  };

  beforeEach(async () => {
    events$ = new Subject<ProcessEvent>();
    extractionApi = {
      extractionResult: vi.fn().mockReturnValue(of(extractionResult)),
      retryExtraction: vi.fn().mockReturnValue(of(extractionResult)),
    };
    retrievalApi = {
      similarDocuments: vi.fn().mockReturnValue(of([])),
      documentTypeCandidates: vi.fn().mockReturnValue(of([])),
      objectDefinitions: vi.fn().mockReturnValue(of([])),
    };
    validationApi = {
      confirm: vi.fn().mockReturnValue(of({ status: 'CONFIRMED' })),
      reject: vi.fn().mockReturnValue(of(undefined)),
    };

    await TestBed.configureTestingModule({
      imports: [ProcessPage],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        { provide: ProcessEventsApi, useValue: { subscribe: () => events$.asObservable() } },
        { provide: ExtractionApi, useValue: extractionApi },
        { provide: RetrievalApi, useValue: retrievalApi },
        { provide: ValidationApi, useValue: validationApi },
      ],
    }).compileComponents();
  });

  function createComponent() {
    const fixture = TestBed.createComponent(ProcessPage);
    fixture.componentRef.setInput('processId', 'p-1');
    fixture.detectChanges();
    return fixture;
  }

  it('starts on the protocol tab with no results loaded', () => {
    const fixture = createComponent();
    expect(fixture.componentInstance['activeTab']()).toBe('PROTOKOLL');
    expect(fixture.componentInstance['resultsReady']()).toBe(false);
  });

  it('loads extraction/retrieval results once EXTRACTED completes and switches to the properties tab', () => {
    const fixture = createComponent();
    events$.next(completedEvent('EXTRACTED'));
    fixture.detectChanges();

    expect(extractionApi.extractionResult).toHaveBeenCalledWith('p-1');
    expect(retrievalApi.documentTypeCandidates).toHaveBeenCalledWith('p-1');
    expect(fixture.componentInstance['extractionResult']()).toEqual(extractionResult);
    expect(fixture.componentInstance['activeTab']()).toBe('EIGENSCHAFTEN');
  });

  it('enables the similar tab as soon as RETRIEVED completes, before extraction', () => {
    const fixture = createComponent();
    events$.next(completedEvent('RETRIEVED'));
    fixture.detectChanges();

    expect(retrievalApi.similarDocuments).toHaveBeenCalledWith('p-1');
    expect(fixture.componentInstance['similarReady']()).toBe(true);
    expect(fixture.componentInstance['resultsReady']()).toBe(false);
  });

  it('only loads results once even if further events arrive', () => {
    const fixture = createComponent();
    events$.next(completedEvent('EXTRACTED'));
    fixture.detectChanges();
    events$.next(completedEvent('EXTRACTED'));
    fixture.detectChanges();

    expect(extractionApi.extractionResult).toHaveBeenCalledOnce();
  });

  it('surfaces an error when loading results fails', () => {
    // NOTE: called directly (not via the EXTRACTED event) — triggering it through the event would
    // leave `isExtractionComplete() && !resultsReady()` true forever, and the component's retry
    // `effect()` would then call `loadResults()` in an infinite loop against the always-failing
    // mock (see finding reported alongside this test file).
    (extractionApi.extractionResult as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => new Error('boom')),
    );
    const fixture = createComponent();
    fixture.componentInstance['loadResults']();

    expect(fixture.componentInstance['actionError']()).toBe(
      'Ergebnisse konnten nicht geladen werden.',
    );
    expect(fixture.componentInstance['loadingResults']()).toBe(false);
  });

  it('confirm() sends the edited attributes and shows a link to the DMS document on success', () => {
    (validationApi.confirm as ReturnType<typeof vi.fn>).mockReturnValue(
      of({ repositoryId: 'repo-1', documentId: 'doc-1', writtenBack: true }),
    );
    const fixture = createComponent();
    events$.next(completedEvent('EXTRACTED'));
    fixture.detectChanges();

    fixture.componentInstance['confirm']();

    expect(validationApi.confirm).toHaveBeenCalledWith(
      expect.objectContaining({ processId: 'p-1', attributes: { p1: 'v1' } }),
    );
    fixture.detectChanges();
    const link = (fixture.nativeElement as HTMLElement).querySelector(
      '.confirmed__link',
    ) as HTMLAnchorElement;
    expect(link.getAttribute('href')).toBe('/dms/r/repo-1/o2/doc-1');
  });

  it('confirm() surfaces an error and re-enables actions on failure', () => {
    (validationApi.confirm as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => new Error('boom')),
    );
    const fixture = createComponent();
    events$.next(completedEvent('EXTRACTED'));
    fixture.detectChanges();

    fixture.componentInstance['confirm']();

    expect(fixture.componentInstance['confirming']()).toBe(false);
    expect(fixture.componentInstance['actionError']()).toBe(
      'Bestaetigen fehlgeschlagen. Bitte erneut versuchen.',
    );
  });

  it('reject() calls the validation API and navigates home on success', () => {
    const fixture = createComponent();
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate');

    fixture.componentInstance['reject']();

    expect(validationApi.reject).toHaveBeenCalledWith({ processId: 'p-1' });
    expect(navigateSpy).toHaveBeenCalledWith(['/']);
  });

  it('onDocumentTypeChange with reExtract=true retries extraction via the API', () => {
    const fixture = createComponent();
    fixture.componentInstance['onDocumentTypeChange']({ documentTypeId: 't2', reExtract: true });

    expect(extractionApi.retryExtraction).toHaveBeenCalledWith('p-1', 't2');
    expect(fixture.componentInstance['selectedDocumentTypeId']()).toBe('t1');
  });

  it('onDocumentTypeChange with reExtract=false clears attributes locally without an API call', () => {
    const fixture = createComponent();
    events$.next(completedEvent('EXTRACTED'));
    fixture.detectChanges();

    fixture.componentInstance['onDocumentTypeChange']({ documentTypeId: 't2', reExtract: false });

    expect(extractionApi.retryExtraction).not.toHaveBeenCalled();
    expect(fixture.componentInstance['extractionResult']()).toEqual({
      documentTypeId: 't2',
      attributes: [],
    });
    expect(fixture.componentInstance['editedAttributes']()).toEqual({});
  });
});
