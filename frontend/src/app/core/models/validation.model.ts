/** `attributes` maps propertyId -> edited value (free text) or values (multi-select), as reviewed by the user. */
export interface ConfirmRequest {
  processId: string;
  documentId: string;
  repositoryId: string;
  consentGiven: boolean;
  templateConsent: boolean;
  attributes: Record<string, string | string[]>;
}

export interface ConfirmationResult {
  documentId: string;
  writtenBack: boolean;
  templateConsentRecorded: boolean;
}

export interface RejectRequest {
  processId: string;
}
