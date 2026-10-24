import { Component, DestroyRef, computed, effect, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ExtractionApi } from '../../core/api/extraction-api';
import { ProcessEventsApi } from '../../core/api/process-events-api';
import { RetrievalApi } from '../../core/api/retrieval-api';
import { ValidationApi } from '../../core/api/validation-api';
import { ExtractionResult } from '../../core/models/extraction.model';
import { ProcessEvent } from '../../core/models/process.model';
import { DmsDocumentMetadata, RetrievalResult } from '../../core/models/retrieval.model';
import { countAttributeReview } from '../../core/attribute-review';
import { findPreviewBlobId, hasFailure, isExtractionComplete } from '../../core/process-progress';
import { DocumentViewer } from './viewer/document-viewer';
import { ProtocolPanel } from './protocol-panel/protocol-panel';
import { PropertiesPanel } from './properties-panel/properties-panel';
import { SimilarPanel } from './similar-panel/similar-panel';
import { StatusBar } from './status-bar/status-bar';

type Tab = 'EIGENSCHAFTEN' | 'AEHNLICHE' | 'PROTOKOLL';

@Component({
  selector: 'app-process-page',
  imports: [DocumentViewer, ProtocolPanel, PropertiesPanel, SimilarPanel, StatusBar],
  templateUrl: './process-page.html',
  styleUrl: './process-page.scss',
})
export class ProcessPage {
  readonly processId = input.required<string>();

  private readonly processEventsApi = inject(ProcessEventsApi);
  private readonly extractionApi = inject(ExtractionApi);
  private readonly retrievalApi = inject(RetrievalApi);
  private readonly validationApi = inject(ValidationApi);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly events = signal<ProcessEvent[]>([]);
  protected readonly extractionResult = signal<ExtractionResult | null>(null);
  protected readonly similarDocuments = signal<RetrievalResult[]>([]);
  protected readonly documentTypeCandidates = signal<DmsDocumentMetadata[]>([]);
  protected readonly editedAttributes = signal<Record<string, string>>({});
  protected readonly selectedDocumentTypeId = signal<string | null>(null);
  protected readonly retryingExtraction = signal(false);
  protected readonly activeTab = signal<Tab>('PROTOKOLL');
  protected readonly loadingResults = signal(false);
  protected readonly confirming = signal(false);
  protected readonly actionError = signal<string | null>(null);

  protected readonly previewBlobId = computed(() => findPreviewBlobId(this.events()));
  protected readonly failed = computed(() => hasFailure(this.events()));
  protected readonly resultsReady = computed(() => this.extractionResult() !== null);

  protected readonly documentType = computed(() => {
    const documentTypeId = this.extractionResult()?.documentTypeId;
    return (
      this.documentTypeCandidates().find(
        (candidate) => candidate.documentType.id === documentTypeId,
      )?.documentType ?? null
    );
  });

  protected readonly reviewCounts = computed(() =>
    countAttributeReview(this.extractionResult()?.attributes ?? [], this.editedAttributes()),
  );

  constructor() {
    effect(() => {
      if (isExtractionComplete(this.events()) && !this.resultsReady() && !this.loadingResults()) {
        this.loadResults();
      }
    });

    // `processId` is stable for this routed component's lifetime (Angular creates a fresh
    // instance per distinct `:processId`), so this effect's subscribe side-effect only fires once.
    effect(() => {
      this.processEventsApi
        .subscribe(this.processId())
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe((event) => this.events.update((list) => [...list, event]));
    });
  }

  private loadResults(): void {
    this.loadingResults.set(true);
    const processId = this.processId();
    forkJoin({
      extraction: this.extractionApi.extractionResult(processId),
      similar: this.retrievalApi.similarDocuments(processId),
      candidates: this.retrievalApi.documentTypeCandidates(processId),
    }).subscribe({
      next: ({ extraction, similar, candidates }) => {
        this.extractionResult.set(extraction);
        this.similarDocuments.set(similar);
        this.documentTypeCandidates.set(candidates);
        this.editedAttributes.set(
          Object.fromEntries(extraction.attributes.map((a) => [a.propertyId, a.value ?? ''])),
        );
        this.selectedDocumentTypeId.set(extraction.documentTypeId);
        this.activeTab.set('EIGENSCHAFTEN');
        this.loadingResults.set(false);
      },
      error: () => {
        this.loadingResults.set(false);
        this.actionError.set('Ergebnisse konnten nicht geladen werden.');
      },
    });
  }

  protected selectTab(tab: Tab): void {
    this.activeTab.set(tab);
  }

  /** Handles a confirmed document-type change from the properties panel's dialog. */
  protected onDocumentTypeChange(change: { documentTypeId: string; reExtract: boolean }): void {
    if (change.reExtract) {
      this.retryExtraction(change.documentTypeId);
    } else {
      this.applyDocumentTypeWithEmptyValues(change.documentTypeId);
    }
  }

  /** Switches to the new document type locally, clearing all property values (no backend call). */
  private applyDocumentTypeWithEmptyValues(documentTypeId: string): void {
    this.extractionResult.set({ documentTypeId, attributes: [] });
    this.editedAttributes.set({});
    this.actionError.set(null);
  }

  private retryExtraction(documentTypeId: string): void {
    if (this.retryingExtraction()) {
      return;
    }
    this.retryingExtraction.set(true);
    this.actionError.set(null);
    this.extractionApi.retryExtraction(this.processId(), documentTypeId).subscribe({
      next: (result) => {
        this.extractionResult.set(result);
        this.editedAttributes.set(
          Object.fromEntries(result.attributes.map((a) => [a.propertyId, a.value ?? ''])),
        );
        this.selectedDocumentTypeId.set(result.documentTypeId);
        this.retryingExtraction.set(false);
      },
      error: () => {
        this.retryingExtraction.set(false);
        this.actionError.set('Erneute Extraktion fehlgeschlagen. Bitte erneut versuchen.');
      },
    });
  }

  protected reject(): void {
    if (this.confirming()) {
      return;
    }
    this.confirming.set(true);
    this.validationApi.reject({ processId: this.processId() }).subscribe({
      next: () => this.router.navigate(['/']),
      error: () => {
        this.confirming.set(false);
        this.actionError.set('Ablehnen fehlgeschlagen. Bitte erneut versuchen.');
      },
    });
  }

  protected confirm(): void {
    const extraction = this.extractionResult();
    if (!extraction || this.confirming()) {
      return;
    }
    this.confirming.set(true);
    this.actionError.set(null);
    // TODO: `documentId`/`repositoryId` aren't resolvable from any GET endpoint for an existing
    // processId (ConfirmRequest needs the DMS location from the original IngestedDocument) — wire
    // through once the import screen passes it along (navigation state or a process lookup
    // endpoint). Confirm will fail against the real backend until this is resolved.
    this.validationApi
      .confirm({
        processId: this.processId(),
        documentId: this.processId(),
        repositoryId: '',
        consentGiven: true,
        templateConsent: false,
        attributes: this.editedAttributes(),
      })
      .subscribe({
        next: () => this.router.navigate(['/']),
        error: () => {
          this.confirming.set(false);
          this.actionError.set('Bestaetigen fehlgeschlagen. Bitte erneut versuchen.');
        },
      });
  }
}
