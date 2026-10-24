export interface ExtractedAttribute {
  propertyId: string;
  value: string | null;
  values: string[] | null;
  confidence: number;
  sourceExcerpt: string | null;
}

export interface ExtractionResult {
  documentTypeId: string | null;
  attributes: ExtractedAttribute[];
}

/** Below this confidence an attribute is flagged `kritisch` (docextract_ascii_layouts.md, Layout 3). */
export const CRITICAL_CONFIDENCE_THRESHOLD = 0.97;
