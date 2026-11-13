import { CRITICAL_CONFIDENCE_THRESHOLD, ExtractedAttribute } from './models/extraction.model';

/** Form value of a property: text for a single value, a list for a multi-value property. */
export type AttributeValue = string | string[];

/** The form value an extracted attribute starts with. */
export function editedValueOf(attribute: ExtractedAttribute): AttributeValue {
  return attribute.values?.length ? attribute.values : (attribute.value ?? '');
}

function sameValue(a: AttributeValue | undefined, b: AttributeValue): boolean {
  if (Array.isArray(a) && Array.isArray(b)) {
    return a.length === b.length && a.every((value, index) => value === b[index]);
  }
  return a === b;
}

export function isCriticalAttribute(attribute: ExtractedAttribute): boolean {
  // A value the DMS set itself is no suggestion to be doubted: it has no confidence (ADR-016).
  if (attribute.origin === 'DMS' || attribute.confidence === null) {
    return false;
  }
  return attribute.confidence < CRITICAL_CONFIDENCE_THRESHOLD;
}

/**
 * Assumption (FRONTEND-SPEC leaves "zu pruefen" undefined): a critical field counts as reviewed
 * once the user has changed it away from the original KI-Vorschlag.
 */
export function isReviewedAttribute(
  attribute: ExtractedAttribute,
  editedValue: AttributeValue | undefined,
): boolean {
  return !isCriticalAttribute(attribute) || !sameValue(editedValue, editedValueOf(attribute));
}

export interface AttributeReviewCounts {
  total: number;
  critical: number;
  toReview: number;
}

export function countAttributeReview(
  attributes: ExtractedAttribute[],
  edited: Record<string, AttributeValue>,
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
