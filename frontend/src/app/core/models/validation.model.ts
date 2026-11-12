import { AttributeOrigin, ValidationError } from './extraction.model';

/** `attributes` maps propertyId -> edited value (free text) or values (multi-select), as reviewed by the user. */
export interface ConfirmRequest {
  processId: string;
  dmsSourceCategory: string | null;
  attributes: Record<string, string | string[]>;
}

/** `indexStatus`: FINALIZED_INDEX_PENDING = im DMS gespeichert, Korpus-Aufnahme folgt per Retry (ADR-015). */
export interface ConfirmationResult {
  documentId: string;
  repositoryId: string;
  writtenBack: boolean;
  indexStatus: 'COMPLETED' | 'FINALIZED_INDEX_PENDING' | 'INDEX_FAILED';
}

export interface RejectRequest {
  processId: string;
}

/** One current value for `/validation/check`; `value` for single, `values` for multi-value. */
export interface CheckAttribute {
  propertyId: string;
  value: string | null;
  values: string[] | null;
  origin: AttributeOrigin | null;
}

export interface CheckRequest {
  processId: string;
  documentTypeId: string;
  attributes: CheckAttribute[];
}

/** A property as the DMS would store it (ADR-016); `changedByDms` also holds for `origin = 'DMS'`. */
export interface ValidatedAttribute {
  propertyId: string;
  value: string | null;
  values: string[] | null;
  origin: AttributeOrigin;
  changedByDms: boolean;
}

/** `attributes` only if `valid`, `error` only if not (ADR-016). */
export interface ValidationReport {
  valid: boolean;
  attributes: ValidatedAttribute[] | null;
  error: ValidationError | null;
}
