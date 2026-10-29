import { ExtractionResult } from '../models/extraction.model';
import { ImportInfo, IngestedDocument } from '../models/ingest.model';
import { ProcessEvent } from '../models/process.model';
import { DmsDocumentMetadata, DmsValueList, RetrievalResult } from '../models/retrieval.model';
import { ConfirmationResult } from '../models/validation.model';

/** Canned responses for `mockApiInterceptor`/`ProcessEventsApi` (mock build configuration only). */

export function mockIngestedDocument(): IngestedDocument {
  return {
    processId: crypto.randomUUID(),
    dmsLocation: 'r/mock-repo/o2/mock-document-1',
    originalBlobId: 'blob-original-mock-1',
    mediaType: 'application/pdf',
  };
}

export function mockImportInfo(): ImportInfo {
  return {
    acceptedFileExtensions: ['.docx', '.jpeg', '.jpg', '.pdf', '.png', '.tif', '.tiff', '.xlsx'],
    maxFileSizeBytes: 50 * 1024 * 1024,
    repositories: [
      {
        id: 'mock-repo',
        name: 'Mock Repository',
        isDefault: true,
        isAvailable: true,
        isReachable: true,
      },
      {
        id: 'mock-archive',
        name: 'Mock Archive',
        isDefault: false,
        isAvailable: true,
        isReachable: true,
      },
    ],
  };
}

/** Replayed with increasing delay by `ProcessEventsApi` in mock mode, mirrors the real SSE order. */
export function mockProcessEvents(processId: string): ProcessEvent[] {
  return [
    { processId, step: 'DMS_UPLOADED', status: 'STARTED', blobId: null },
    { processId, step: 'DMS_UPLOADED', status: 'COMPLETED', blobId: null },
    { processId, step: 'PREVIEW', status: 'STARTED', blobId: null },
    { processId, step: 'PREVIEW', status: 'COMPLETED', blobId: 'blob-preview-mock-1' },
    { processId, step: 'TEXT_EXTRACTED', status: 'STARTED', blobId: null },
    { processId, step: 'TEXT_EXTRACTED', status: 'COMPLETED', blobId: null },
    { processId, step: 'RETRIEVED', status: 'STARTED', blobId: null },
    { processId, step: 'RETRIEVED', status: 'COMPLETED', blobId: null },
    { processId, step: 'EXTRACTED', status: 'STARTED', blobId: null },
    { processId, step: 'EXTRACTED', status: 'COMPLETED', blobId: null },
  ];
}

const invoiceNumberProperty = {
  id: 'property_invoice_number',
  name: 'Rechnungsnummer',
  dataType: 'ALPHANUMERIC' as const,
  mandatory: true,
  hasValueList: false,
  isDynamicValueList: false,
  isHidden: false,
  isModifiable: true,
  isSystemProperty: false,
  isMultiAttribute: false,
  isDocumentTypeProperty: true,
  maxRowCount: 1,
  maxLength: 40,
  valueList: [],
  hasMoreValues: false,
};

const invoiceDateProperty = {
  id: 'property_invoice_date',
  name: 'Rechnungsdatum',
  dataType: 'DATE' as const,
  mandatory: true,
  hasValueList: false,
  isDynamicValueList: false,
  isHidden: false,
  isModifiable: true,
  isSystemProperty: false,
  isMultiAttribute: false,
  isDocumentTypeProperty: true,
  maxRowCount: 1,
  maxLength: null,
  valueList: [],
  hasMoreValues: false,
};

const supplierProperty = {
  id: 'property_supplier',
  name: 'Lieferant',
  dataType: 'KEY_VALUE_LIST' as const,
  mandatory: false,
  hasValueList: true,
  isDynamicValueList: true,
  isHidden: false,
  isModifiable: true,
  isSystemProperty: false,
  isMultiAttribute: false,
  isDocumentTypeProperty: true,
  maxRowCount: 1,
  maxLength: null,
  valueList: ['Muster AG', 'Beispiel GmbH'],
  hasMoreValues: false,
};

const contractTitleProperty = {
  id: 'property_contract_title',
  name: 'Vertragstitel',
  dataType: 'ALPHANUMERIC' as const,
  mandatory: true,
  hasValueList: false,
  isDynamicValueList: false,
  isHidden: false,
  isModifiable: true,
  isSystemProperty: false,
  isMultiAttribute: false,
  isDocumentTypeProperty: true,
  maxRowCount: 1,
  maxLength: 80,
  valueList: [],
  hasMoreValues: false,
};

const contractPartnerProperty = {
  id: 'property_contract_partner',
  name: 'Vertragspartner',
  dataType: 'ALPHANUMERIC' as const,
  mandatory: false,
  hasValueList: false,
  isDynamicValueList: false,
  isHidden: false,
  isModifiable: true,
  isSystemProperty: false,
  isMultiAttribute: false,
  isDocumentTypeProperty: true,
  maxRowCount: 1,
  maxLength: 80,
  valueList: [],
  hasMoreValues: false,
};

/** Repository document types; only the invoice has similar documents (score > 0). */
export function mockDocumentTypeCandidates(): DmsDocumentMetadata[] {
  return [
    {
      documentType: {
        id: 'doctype_invoice',
        name: 'Rechnung',
        properties: [invoiceNumberProperty, invoiceDateProperty, supplierProperty],
        score: 1.74,
      },
      contentLanguage: 'de',
      properties: [],
    },
    {
      documentType: {
        id: 'doctype_contract',
        name: 'Vertrag',
        properties: [contractTitleProperty, contractPartnerProperty],
        score: 0,
      },
      contentLanguage: 'de',
      properties: [],
    },
    {
      documentType: { id: 'doctype_letter', name: 'Brief', properties: [], score: 0 },
      contentLanguage: 'de',
      properties: [],
    },
    {
      documentType: { id: 'doctype_delivery_note', name: 'Lieferschein', properties: [], score: 0 },
      contentLanguage: 'de',
      properties: [],
    },
  ];
}

export function mockSimilarDocuments(): RetrievalResult[] {
  return [
    {
      repositoryId: 'mock-repo',
      documentId: 'mock-document-2',
      score: 0.93,
      properties: JSON.stringify({
        documentType: { id: 'doctype_invoice', name: 'Rechnung' },
        properties: [{ id: 'property_supplier', value: 'Muster AG' }],
      }),
    },
    {
      repositoryId: 'mock-repo',
      documentId: 'mock-document-3',
      score: 0.81,
      properties: JSON.stringify({
        documentType: { id: 'doctype_invoice', name: 'Rechnung' },
        properties: [{ id: 'property_supplier', value: 'Beispiel GmbH' }],
      }),
    },
  ];
}

export function mockExtractionResult(): ExtractionResult {
  return {
    documentTypeId: 'doctype_invoice',
    attributes: [
      {
        propertyId: 'property_invoice_number',
        value: 'RE-2026-00142',
        values: null,
        confidence: 0.99,
        sourceExcerpt: 'Rechnungsnummer: RE-2026-00142',
      },
      {
        propertyId: 'property_invoice_date',
        value: '2026-10-01',
        values: null,
        confidence: 0.95,
        sourceExcerpt: 'Datum: 01.10.2026',
      },
      {
        propertyId: 'property_supplier',
        value: 'Muster AG',
        values: null,
        confidence: 0.72,
        sourceExcerpt: 'Muster AG, Musterstrasse 1',
      },
    ],
  };
}

export function mockValueList(): DmsValueList {
  return { values: ['Muster AG', 'Beispiel GmbH'], hasMore: false };
}

/** Returns different attributes depending on the reviewer-chosen `documentTypeId` (retry flow). */
export function mockRetryExtractionResult(documentTypeId: string): ExtractionResult {
  if (documentTypeId === 'doctype_contract') {
    return {
      documentTypeId: 'doctype_contract',
      attributes: [
        {
          propertyId: 'property_contract_title',
          value: 'Dienstleistungsvertrag',
          values: null,
          confidence: 0.91,
          sourceExcerpt: 'Dienstleistungsvertrag zwischen Muster AG und Beispiel GmbH',
        },
        {
          propertyId: 'property_contract_partner',
          value: 'Beispiel GmbH',
          values: null,
          confidence: 0.88,
          sourceExcerpt: 'Vertragspartner: Beispiel GmbH',
        },
      ],
    };
  }
  return mockExtractionResult();
}

export function mockConfirmationResult(): ConfirmationResult {
  return { repositoryId: 'mock-repo', documentId: 'mock-document-1', writtenBack: true };
}
