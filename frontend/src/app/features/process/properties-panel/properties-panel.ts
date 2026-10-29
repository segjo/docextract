import { Component, computed, input, model, output, signal } from '@angular/core';
import { ExtractedAttribute } from '../../../core/models/extraction.model';
import {
  DmsDocumentMetadata,
  DmsDocumentType,
  DmsPropertyDefinition,
} from '../../../core/models/retrieval.model';
import { isCriticalAttribute, isReviewedAttribute } from '../../../core/attribute-review';

interface AttributeRow {
  attribute: ExtractedAttribute;
  definition: DmsPropertyDefinition | undefined;
  critical: boolean;
  reviewed: boolean;
  /** True if the property has no value in the extraction result (not returned by the KI extraction). */
  empty: boolean;
  mandatory: boolean;
}

@Component({
  selector: 'app-properties-panel',
  templateUrl: './properties-panel.html',
  styleUrl: './properties-panel.scss',
})
export class PropertiesPanel {
  readonly documentType = input<DmsDocumentType | null>(null);
  /** The process's staged document-type candidates (SPEC §3), for the reviewer to pick from. */
  readonly documentTypeCandidates = input<DmsDocumentMetadata[]>([]);
  /** All document types from /object-definitions; the full option list of the select. */
  readonly objectDefinitions = input<DmsDocumentMetadata[]>([]);
  /** Two-way bound with the container: the currently selected candidate's document type id. */
  readonly selectedDocumentTypeId = model<string | null>(null);
  /** True while a retry extraction request is in flight; disables the dialog's actions. */
  readonly retrying = input(false);
  readonly extractedAttributes = input.required<ExtractedAttribute[]>();
  /** Edited values, two-way bound with the container so Confirm/Reject see the reviewed state. */
  readonly attributes = model.required<Record<string, string>>();
  /** Critical properties the reviewer has verified without changing their value. */
  readonly acknowledgedIds = model<string[]>([]);

  /**
   * Emitted once the reviewer confirms a document type change in the dialog: `reExtract` selects
   * between the two confirm actions — clear the properties for the new type, or re-run extraction
   * against it.
   */
  readonly documentTypeChange = output<{ documentTypeId: string; reExtract: boolean }>();

  protected readonly expandedSourceId = model<string | null>(null);
  /** Toggled by the "leere Eigenschaften anzeigen" checkbox; mandatory properties are always shown. */
  protected readonly showEmptyProperties = signal(false);

  /** Candidate just picked in the `<select>`, awaiting confirmation in the dialog; `null` = closed. */
  protected readonly pendingDocumentTypeId = signal<string | null>(null);
  /** Drives the `<select>`'s displayed value: the pending pick while the dialog is open, else the
   * committed selection — so a Cancel visually reverts the `<select>` to the current type. */
  protected readonly displayedDocumentTypeId = computed(
    () => this.pendingDocumentTypeId() ?? this.selectedDocumentTypeId(),
  );
  protected readonly candidateIds = computed(
    () => new Set(this.documentTypeCandidates().map((candidate) => candidate.documentType.id)),
  );
  /** Object definitions plus any candidate not (yet) contained in them. */
  protected readonly documentTypeOptions = computed(() => {
    const all = this.objectDefinitions();
    const known = new Set(all.map((entry) => entry.documentType.id));
    return [...all, ...this.documentTypeCandidates().filter((c) => !known.has(c.documentType.id))];
  });
  protected readonly pendingCandidate = computed(() =>
    this.documentTypeOptions().find(
      (candidate) => candidate.documentType.id === this.pendingDocumentTypeId(),
    ),
  );

  protected readonly rows = computed<AttributeRow[]>(() => {
    const definitions = this.documentType()?.properties ?? [];
    const edited = this.attributes();
    const acknowledged = this.acknowledgedIds();
    const extractedAttributes = this.extractedAttributes();
    const extractedIds = new Set(extractedAttributes.map((attribute) => attribute.propertyId));

    const extractedRows = extractedAttributes.map((attribute) => {
      const definition = definitions.find((candidate) => candidate.id === attribute.propertyId);
      return {
        attribute,
        definition,
        critical: isCriticalAttribute(attribute),
        reviewed: isReviewedAttribute(
          attribute,
          edited[attribute.propertyId],
          acknowledged.includes(attribute.propertyId),
        ),
        empty: false,
        mandatory: definition?.mandatory ?? false,
      };
    });

    const showEmpty = this.showEmptyProperties();
    const emptyRows = definitions
      .filter(
        (definition) => !extractedIds.has(definition.id) && (definition.mandatory || showEmpty),
      )
      .map((definition) => ({
        attribute: {
          propertyId: definition.id,
          value: null,
          values: null,
          confidence: 1,
          sourceExcerpt: null,
        },
        definition,
        critical: false,
        reviewed: true,
        empty: true,
        mandatory: definition.mandatory,
      }));

    return [...extractedRows, ...emptyRows];
  });

  protected updateValue(propertyId: string, value: string): void {
    this.attributes.update((current) => ({ ...current, [propertyId]: value }));
  }

  protected toggleAcknowledged(propertyId: string, checked: boolean): void {
    this.acknowledgedIds.update((current) =>
      checked
        ? [...current.filter((id) => id !== propertyId), propertyId]
        : current.filter((id) => id !== propertyId),
    );
  }

  protected selectDocumentType(documentTypeId: string): void {
    if (!documentTypeId || documentTypeId === this.selectedDocumentTypeId()) {
      return;
    }
    this.pendingDocumentTypeId.set(documentTypeId);
  }

  protected cancelDocumentTypeChange(): void {
    this.pendingDocumentTypeId.set(null);
  }

  protected confirmDocumentTypeChange(reExtract: boolean): void {
    const documentTypeId = this.pendingDocumentTypeId();
    if (!documentTypeId || this.retrying()) {
      return;
    }
    this.selectedDocumentTypeId.set(documentTypeId);
    this.pendingDocumentTypeId.set(null);
    this.documentTypeChange.emit({ documentTypeId, reExtract });
  }

  protected toggleShowEmptyProperties(value: boolean): void {
    this.showEmptyProperties.set(value);
  }

  protected toggleSource(propertyId: string): void {
    this.expandedSourceId.update((current) => (current === propertyId ? null : propertyId));
  }

  protected confidencePercent(confidence: number): number {
    return Math.round(confidence * 100);
  }
}
