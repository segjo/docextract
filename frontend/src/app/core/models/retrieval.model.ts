export type DmsPropertyDataType =
  | 'ALPHANUMERIC'
  | 'NUMERIC'
  | 'MONEY'
  | 'DATETIME'
  | 'DATE'
  | 'KEY_VALUE_LIST'
  | 'COLOR_CODE'
  | 'DOCUMENT_TYPE'
  | 'UNKNOWN';

/** Schema of a single DMS property, shared by a document type and its instance values. */
export interface DmsPropertyDefinition {
  id: string;
  name: string;
  dataType: DmsPropertyDataType;
  mandatory: boolean;
  hasValueList: boolean;
  isDynamicValueList: boolean;
  isHidden: boolean;
  isModifiable: boolean;
  isSystemProperty: boolean;
  isMultiAttribute: boolean;
  isDocumentTypeProperty: boolean;
  maxRowCount: number | null;
  maxLength: number | null;
  valueList: string[];
  hasMoreValues: boolean;
}

export interface DmsDocumentType {
  id: string;
  name: string;
  properties: DmsPropertyDefinition[];
}

/** A property *value* on a document instance — references its schema via `id` (DmsDocumentType.properties). */
export interface DmsProperty {
  id: string;
  value: string | null;
  values: string[] | null;
}

export interface DmsDocumentMetadata {
  documentType: DmsDocumentType;
  contentLanguage: string | null;
  properties: DmsProperty[];
}

/** One hit from the similar-documents retrieval (FR-3); `properties` is a raw JSON string. */
export interface RetrievalResult {
  repositoryId: string;
  documentId: string;
  score: number;
  properties: string | null;
}

export interface ValueListRequest {
  objectDefinitionId: string;
  extendedProperties: Record<string, string>;
  multivalueExtendedProperties: Record<string, string[]>;
  searchTerm: string | null;
}

export interface DmsValueList {
  values: string[];
  hasMore: boolean;
}
