/** `attributes` maps propertyId -> edited value (free text) or values (multi-select), as reviewed by the user. */
export interface ConfirmRequest {
  processId: string;
  dmsSourceCategory: string | null;
  attributes: Record<string, string | string[]>;
}

export interface ConfirmationResult {
  documentId: string;
  repositoryId: string;
  writtenBack: boolean;
}

export interface RejectRequest {
  processId: string;
}
