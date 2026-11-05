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
  indexStatus: 'COMPLETED' | 'FINALIZED_INDEX_PENDING';
}

export interface RejectRequest {
  processId: string;
}
