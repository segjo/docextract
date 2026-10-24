import { CRITICAL_CONFIDENCE_THRESHOLD, ExtractedAttribute } from './models/extraction.model';

export function isCriticalAttribute(attribute: ExtractedAttribute): boolean {
  return attribute.confidence < CRITICAL_CONFIDENCE_THRESHOLD;
}

/**
 * Assumption (FRONTEND-SPEC leaves "zu pruefen" undefined): a critical field counts as reviewed
 * once the user has changed it away from the original KI-Vorschlag.
 */
export function isReviewedAttribute(
  attribute: ExtractedAttribute,
  editedValue: string | undefined,
): boolean {
  return !isCriticalAttribute(attribute) || editedValue !== (attribute.value ?? '');
}

export interface AttributeReviewCounts {
  total: number;
  critical: number;
  toReview: number;
}

export function countAttributeReview(
  attributes: ExtractedAttribute[],
  edited: Record<string, string>,
): AttributeReviewCounts {
  let critical = 0;
  let toReview = 0;
  for (const attribute of attributes) {
    if (isCriticalAttribute(attribute)) {
      critical++;
      if (!isReviewedAttribute(attribute, edited[attribute.propertyId])) {
        toReview++;
      }
    }
  }
  return { total: attributes.length, critical, toReview };
}
