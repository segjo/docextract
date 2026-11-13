import { Component, computed, input, model, output, signal } from '@angular/core';
import { ExtractedAttribute } from '../../../core/models/extraction.model';
import {
  DmsDocumentMetadata,
  DmsDocumentType,
  DmsPropertyDefinition,
} from '../../../core/models/retrieval.model';
import { ValidationReport } from '../../../core/models/validation.model';
import {
  AttributeValue,
  isCriticalAttribute,
  isReviewedAttribute,
} from '../../../core/attribute-review';
import { MultiValueDialog } from '../multi-value-dialog/multi-value-dialog';

/** What the DMS did to a value (ADR-016): `DMS_SET` = set by a hook/default, `DMS_CHANGED` = adjusted. */
type DmsMark = 'DMS_SET' | 'DMS_CHANGED' | 'SIMILAR' | null;

interface AttributeRow {
  attribute: ExtractedAttribute;
  definition: DmsPropertyDefinition | undefined;
  /** Multi-value property: edited as a list. */
  multiple: boolean;
  /** Current form value as a list (one entry for a single value, none if empty). */
  selected: string[];
  /** Value list plus current values missing from it, so a suggestion is always shown. */
  options: string[];
  critical: boolean;
  reviewed: boolean;
  /** True if the property has no value in the extraction result (not returned by the KI extraction). */
  empty: boolean;
  mandatory: boolean;
  dmsMark: DmsMark;
  /** The LLM changed the value after the DMS rejected an earlier one (confidence is capped). */
  corrected: boolean;
  /** The DMS message for this field from the last check, if it rejected it. */
  error: string | null;
}

@Component({
  selector: 'app-properties-panel',
  imports: [MultiValueDialog],
  templateUrl: './properties-panel.html',
  styleUrl: './properties-panel.scss',
})
export class PropertiesPanel {
  readonly documentType = input<DmsDocumentType | null>(null);
  /** The process's staged document-type candidates (SPEC §3), for the reviewer to pick from. */
  readonly documentTypeCandidates = input<DmsDocumentMetadata[]>([]);
  /** Two-way bound with the container: the currently selected candidate's document type id. */
  readonly selectedDocumentTypeId = model<string | null>(null);
  /** True while a retry extraction request is in flight; disables the dialog's actions. */
  readonly retrying = input(false);
  readonly extractedAttributes = input.required<ExtractedAttribute[]>();
  /** Edited values, two-way bound with the container so Confirm/Reject see the reviewed state. */
  readonly attributes = model.required<Record<string, AttributeValue>>();
  /** True while a "Im DMS prüfen" request is in flight. */
  readonly checking = input(false);
  /** Result of the last DMS check (or of the extraction's final check); `null` = none yet. */
  readonly checkReport = input<ValidationReport | null>(null);

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
  protected readonly pendingCandidate = computed(() =>
    this.documentTypeCandidates().find(
      (candidate) => candidate.documentType.id === this.pendingDocumentTypeId(),
    ),
  );

  protected readonly multiValueDialogOpen = signal(false);

  /** All editable multi-value properties of the type: one table column each, as they may depend on each other. */
  protected readonly multiValueDefinitions = computed(() =>
    (this.documentType()?.properties ?? []).filter(
      (definition) =>
        definition.isMultiAttribute &&
        definition.isModifiable &&
        !definition.isHidden &&
        !definition.isSystemProperty,
    ),
  );

  protected readonly multiValues = computed(() => {
    const edited = this.attributes();
    return Object.fromEntries(
      this.multiValueDefinitions().map((definition) => [
        definition.id,
        toList(edited[definition.id]),
      ]),
    );
  });

  /** The error of the last check; `propertyId` only set for a field of this document type. */
  private readonly checkError = computed(() => this.checkReport()?.error ?? null);

  protected readonly checkInvalid = computed(() => this.checkReport()?.valid === false);

  /** Text of the live region: announced to screen readers when it changes (NfA-F1). */
  protected readonly checkMessage = computed(() => {
    if (this.checking()) {
      return 'Prüfe im DMS ...';
    }
    const report = this.checkReport();
    if (!report) {
      return '';
    }
    if (report.valid) {
      return 'Im DMS geprüft: gültig.';
    }
    const error = report.error;
    const definition = this.documentType()?.properties.find((d) => d.id === error?.propertyId);
    if (definition) {
      return `Im DMS geprüft: Fehler bei „${definition.name}“.`;
    }
    return `Im DMS geprüft: ${error?.message ?? 'Eingabe abgelehnt.'}`;
  });

  protected readonly rows = computed<AttributeRow[]>(() => {
    const definitions = this.documentType()?.properties ?? [];
    const edited = this.attributes();
    const extractedAttributes = this.extractedAttributes();
    const extractedIds = new Set(extractedAttributes.map((attribute) => attribute.propertyId));
    const error = this.checkError();
    const errorPropertyId =
      error?.propertyId && definitions.some((d) => d.id === error.propertyId)
        ? error.propertyId
        : null;
    const errorFor = (propertyId: string): string | null =>
      propertyId === errorPropertyId ? (error?.message ?? 'Vom DMS abgelehnt.') : null;

    const extractedRows = extractedAttributes.map((attribute) => {
      const definition = definitions.find((candidate) => candidate.id === attribute.propertyId);
      return {
        attribute,
        definition,
        ...fieldState(definition, edited[attribute.propertyId]),
        critical: isCriticalAttribute(attribute),
        reviewed: isReviewedAttribute(attribute, edited[attribute.propertyId]),
        empty: false,
        mandatory: definition?.mandatory ?? false,
        dmsMark: dmsMarkOf(attribute),
        corrected: attribute.correctedAfterValidation === true,
        error: errorFor(attribute.propertyId),
      };
    });

    const showEmpty = this.showEmptyProperties();
    const emptyRows = definitions
      .filter(
        (definition) =>
          !extractedIds.has(definition.id) &&
          (definition.mandatory ||
            showEmpty ||
            definition.id === errorPropertyId ||
            toList(edited[definition.id]).length > 0),
      )
      .map((definition) => ({
        attribute: {
          propertyId: definition.id,
          value: null,
          values: null,
          confidence: null,
          sourceExcerpt: null,
        },
        definition,
        ...fieldState(definition, edited[definition.id]),
        critical: false,
        reviewed: true,
        empty: toList(edited[definition.id]).length === 0,
        mandatory: definition.mandatory,
        dmsMark: null,
        corrected: false,
        error: errorFor(definition.id),
      }));

    return [...extractedRows, ...emptyRows];
  });

  protected updateValue(propertyId: string, value: AttributeValue): void {
    this.attributes.update((current) => ({ ...current, [propertyId]: value }));
  }

  protected saveMultiValues(values: Record<string, string[]>): void {
    this.attributes.update((current) => ({ ...current, ...values }));
    this.multiValueDialogOpen.set(false);
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

  protected confidencePercent(confidence: number | null): number | null {
    return confidence === null ? null : Math.round(confidence * 100);
  }
}

function toList(value: AttributeValue | undefined): string[] {
  return Array.isArray(value) ? value : value ? [value] : [];
}

function fieldState(
  definition: DmsPropertyDefinition | undefined,
  value: AttributeValue | undefined,
): Pick<AttributeRow, 'multiple' | 'selected' | 'options'> {
  const selected = toList(value);
  const valueList = definition?.valueList ?? [];
  return {
    multiple: definition?.isMultiAttribute ?? Array.isArray(value),
    selected,
    options: [...valueList, ...selected.filter((entry) => !valueList.includes(entry))],
  };
}

function dmsMarkOf(attribute: ExtractedAttribute): DmsMark {
  if (attribute.origin === 'DMS') {
    return 'DMS_SET';
  }
  if (attribute.origin === 'SIMILAR') {
    return 'SIMILAR';
  }
  return attribute.changedByDms === true ? 'DMS_CHANGED' : null;
}
