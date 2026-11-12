import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, effect, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ExtractionApi } from '../../core/api/extraction-api';
import { ProcessEventsApi } from '../../core/api/process-events-api';
import { RetrievalApi } from '../../core/api/retrieval-api';
import { ValidationApi } from '../../core/api/validation-api';
import { ConfirmationResult, ValidationReport } from '../../core/models/validation.model';
import { ExtractionResult } from '../../core/models/extraction.model';
import { ProcessEvent } from '../../core/models/process.model';
import { DmsDocumentMetadata, RetrievalResult } from '../../core/models/retrieval.model';
import { countAttributeReview } from '../../core/attribute-review';
import { normalizeDateAttributes } from '../../core/date-format';
import { applyValidatedAttributes, buildCheckAttributes } from '../../core/validation-check';
import {
  findPreviewBlobId,
  hasFailure,
  isExtractionComplete,
  isRetrievalComplete,
} from '../../core/process-progress';
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
  protected readonly acknowledgedIds = signal<string[]>([]);
  protected readonly selectedDocumentTypeId = signal<string | null>(null);
  protected readonly retryingExtraction = signal(false);
  protected readonly activeTab = signal<Tab>('PROTOKOLL');
  protected readonly loadingResults = signal(false);
  protected readonly confirming = signal(false);
  protected readonly checking = signal(false);
  /** Last DMS check, or the error of the extraction's final check; never blocks the release. */
  protected readonly checkReport = signal<ValidationReport | null>(null);
  protected readonly confirmation = signal<ConfirmationResult | null>(null);
  protected readonly actionError = signal<string | null>(null);

  protected readonly previewBlobId = computed(() => findPreviewBlobId(this.events()));
  protected readonly failed = computed(() => hasFailure(this.events()));
  protected readonly similarReady = signal(false);
  private loadingSimilar = false;
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
    countAttributeReview(
      this.extractionResult()?.attributes ?? [],
      this.editedAttributes(),
      this.acknowledgedIds(),
    ),
  );

  constructor() {
    effect(() => {
      if (isRetrievalComplete(this.events()) && !this.similarReady() && !this.loadingSimilar) {
        this.loadSimilar();
      }
    });

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
        .subscribe((event) =>
          this.events.update((list) => [...list, { ...event, receivedAt: Date.now() }]),
        );
    });
  }

  private loadSimilar(): void {
    this.loadingSimilar = true;
    this.retrievalApi.similarDocuments(this.processId()).subscribe({
      next: (similar) => {
        this.similarDocuments.set(similar);
        this.similarReady.set(true);
        this.loadingSimilar = false;
      },
      error: () => {
        this.loadingSimilar = false;
      },
    });
  }

  private loadResults(): void {
    this.loadingResults.set(true);
    const processId = this.processId();
    forkJoin({
      extraction: this.extractionApi.extractionResult(processId),
      candidates: this.retrievalApi.documentTypeCandidates(processId),
    }).subscribe({
      next: ({ extraction, candidates }) => {
        this.extractionResult.set(extraction);
        this.documentTypeCandidates.set(candidates);
        this.checkReport.set(initialReport(extraction));
        this.acknowledgedIds.set([]);
        this.editedAttributes.set(
          Object.fromEntries(extraction.attributes.map((a) => [a.propertyId, a.value ?? ''])),
        );
        this.selectedDocumentTypeId.set(extraction.documentTypeId);
        if (this.activeTab() === 'PROTOKOLL') {
          this.activeTab.set('EIGENSCHAFTEN');
        }
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
    this.checkReport.set(null);
    this.acknowledgedIds.set([]);
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
        this.checkReport.set(initialReport(result));
        this.acknowledgedIds.set([]);
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

  /**
   * Read-only DMS check of the current form values (ADR-016). Valid: what the DMS would store
   * replaces the form values and marks what it set or changed. Invalid: the error is shown at its
   * field, or generally. No effect on `confirm`: the release stays possible in every state (C-2).
   */
  protected checkInDms(): void {
    const extraction = this.extractionResult();
    const documentTypeId = this.selectedDocumentTypeId();
    if (!extraction || !documentTypeId || this.checking()) {
      return;
    }
    this.checking.set(true);
    this.actionError.set(null);
    const edited = normalizeDateAttributes(
      this.editedAttributes(),
      this.documentType()?.properties ?? [],
    );
    this.validationApi
      .check({
        processId: this.processId(),
        documentTypeId,
        attributes: buildCheckAttributes(edited, extraction.attributes),
      })
      .subscribe({
        next: (report) => {
          this.checking.set(false);
          this.checkReport.set(report);
          if (report.valid && report.attributes) {
            const applied = applyValidatedAttributes(
              extraction,
              this.editedAttributes(),
              report.attributes,
            );
            this.extractionResult.set(applied.result);
            this.editedAttributes.set(applied.edited);
          }
        },
        error: (err: HttpErrorResponse) => {
          this.checking.set(false);
          this.checkReport.set(null);
          this.actionError.set(
            err.status === 502
              ? 'Das DMS ist nicht erreichbar. Die Prüfung war nicht möglich.'
              : 'Die Prüfung im DMS ist fehlgeschlagen. Bitte erneut versuchen.',
          );
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
    this.validationApi
      .confirm({
        processId: this.processId(),
        dmsSourceCategory: this.selectedDocumentTypeId(),
        attributes: normalizeDateAttributes(
          this.editedAttributes(),
          this.documentType()?.properties ?? [],
        ),
      })
      .subscribe({
        next: (result) => {
          this.confirming.set(false);
          this.confirmation.set(result);
        },
        error: (err: HttpErrorResponse) => {
          this.confirming.set(false);
          const reason = err.status === 422 ? err.error?.detail : null;
          this.actionError.set(reason || 'Bestaetigen fehlgeschlagen. Bitte erneut versuchen.');
        },
      });
  }

  protected importNext(): void {
    this.router.navigate(['/']);
  }
}

/** The error of the extraction's final DMS check is shown like one of a manual check. */
function initialReport(result: ExtractionResult): ValidationReport | null {
  return result.validation?.status === 'INVALID'
    ? { valid: false, attributes: null, error: result.validation.error }
    : null;
}
