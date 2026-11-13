/**
 * `LLM` = nicht vom DMS gesetzt, `DMS` = von einem Hook/Default des DMS gesetzt (ADR-016),
 * `SIMILAR` = aus den Werten aehnlicher Dokumente vorgeschlagen (nicht im Dokument gefunden).
 */
export type AttributeOrigin = 'LLM' | 'DMS' | 'SIMILAR';

/**
 * `confidence` is 0-1, `null` for `origin = 'DMS'`. The ADR-016 fields are absent in results stored
 * before it: read as `origin = 'LLM'` and both flags `false`.
 */
export interface ExtractedAttribute {
  propertyId: string;
  value: string | null;
  values: string[] | null;
  confidence: number | null;
  sourceExcerpt: string | null;
  origin?: AttributeOrigin;
  changedByDms?: boolean;
  /** The LLM changed the value after the DMS rejected an earlier one; confidence is capped. */
  correctedAfterValidation?: boolean;
}

/** The one error the DMS reports per check; `propertyId` only for writable properties. */
export interface ValidationError {
  propertyId: string | null;
  dmsCode: string | null;
  message: string | null;
}

/** Result of the final DMS check of the extraction; absent in older results = `NOT_CHECKED`. */
export interface ValidationOutcome {
  status: 'VALID' | 'INVALID' | 'NOT_CHECKED';
  error: ValidationError | null;
}

export interface ExtractionResult {
  documentTypeId: string | null;
  attributes: ExtractedAttribute[];
  validation?: ValidationOutcome;
}

/** Below this confidence an attribute is flagged `kritisch` (docextract_ascii_layouts.md, Layout 3). */
export const CRITICAL_CONFIDENCE_THRESHOLD = 0.97;
