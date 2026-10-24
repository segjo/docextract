import { Component, computed, input } from '@angular/core';
import { DmsDocumentMetadata, RetrievalResult } from '../../../core/models/retrieval.model';

interface SimilarDocumentRow {
  result: RetrievalResult;
  metadata: DmsDocumentMetadata | null;
}

@Component({
  selector: 'app-similar-panel',
  templateUrl: './similar-panel.html',
  styleUrl: './similar-panel.scss',
})
export class SimilarPanel {
  readonly results = input.required<RetrievalResult[]>();

  protected readonly rows = computed<SimilarDocumentRow[]>(() =>
    this.results().map((result) => ({ result, metadata: parseMetadata(result.properties) })),
  );

  protected scorePercent(score: number): number {
    return Math.round(score * 100);
  }
}

function parseMetadata(raw: string | null): DmsDocumentMetadata | null {
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as DmsDocumentMetadata;
  } catch {
    return null;
  }
}
