import { Component, computed, input } from '@angular/core';
import { DmsDocumentMetadata, RetrievalResult } from '../../../core/models/retrieval.model';

interface SimilarDocumentRow {
  result: RetrievalResult;
  metadata: DmsDocumentMetadata | null;
  title: string;
  properties: PropertyRow[];
}

interface PropertyRow {
  name: string;
  value: string;
}

@Component({
  selector: 'app-similar-panel',
  templateUrl: './similar-panel.html',
  styleUrl: './similar-panel.scss',
})
export class SimilarPanel {
  readonly results = input.required<RetrievalResult[]>();

  protected readonly rows = computed<SimilarDocumentRow[]>(() =>
    this.results().map((result) => {
      const metadata = parseMetadata(result.properties);
      return { result, metadata, title: toTitle(metadata), properties: toPropertyRows(metadata) };
    }),
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

function toTitle(metadata: DmsDocumentMetadata | null): string {
  const caption = metadata?.properties?.find((p) => p.id === 'property_caption')?.value?.trim();
  return caption || (metadata?.documentType?.name ?? 'Dokument');
}

/** Resolves property names via the document type schema and drops properties without a value. */
function toPropertyRows(metadata: DmsDocumentMetadata | null): PropertyRow[] {
  const names = new Map(
    (metadata?.documentType?.properties ?? []).map((definition) => [
      definition.id,
      definition.name,
    ]),
  );
  return (metadata?.properties ?? []).flatMap((property) => {
    const value = property.values?.length ? property.values.join(', ') : (property.value ?? '');
    return value.trim() ? [{ name: names.get(property.id) ?? property.id, value }] : [];
  });
}
