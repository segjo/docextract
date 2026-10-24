import { ExtractedAttribute } from './models/extraction.model';
import { countAttributeReview, isCriticalAttribute, isReviewedAttribute } from './attribute-review';

function attribute(partial: Partial<ExtractedAttribute>): ExtractedAttribute {
  return {
    propertyId: 'prop-1',
    value: 'Rechnung',
    values: null,
    confidence: 1,
    sourceExcerpt: null,
    ...partial,
  };
}

describe('isCriticalAttribute', () => {
  it('is false at/above the 0.97 threshold', () => {
    expect(isCriticalAttribute(attribute({ confidence: 0.97 }))).toBe(false);
    expect(isCriticalAttribute(attribute({ confidence: 1 }))).toBe(false);
  });

  it('is true below the 0.97 threshold', () => {
    expect(isCriticalAttribute(attribute({ confidence: 0.96 }))).toBe(true);
  });
});

describe('isReviewedAttribute', () => {
  it('a non-critical attribute always counts as reviewed', () => {
    expect(isReviewedAttribute(attribute({ confidence: 1, value: 'x' }), 'x')).toBe(true);
  });

  it('a critical attribute is unreviewed while its value is unchanged', () => {
    const attr = attribute({ confidence: 0.5, value: 'x' });
    expect(isReviewedAttribute(attr, 'x')).toBe(false);
  });

  it('a critical attribute becomes reviewed once its value is edited', () => {
    const attr = attribute({ confidence: 0.5, value: 'x' });
    expect(isReviewedAttribute(attr, 'y')).toBe(true);
  });

  it('treats a null original value as an empty string for comparison', () => {
    const attr = attribute({ confidence: 0.5, value: null });
    expect(isReviewedAttribute(attr, '')).toBe(false);
    expect(isReviewedAttribute(attr, 'anything')).toBe(true);
  });
});

describe('countAttributeReview', () => {
  it('counts total/critical/toReview across all attributes', () => {
    const attributes = [
      attribute({ propertyId: 'a', confidence: 1, value: 'ok' }),
      attribute({ propertyId: 'b', confidence: 0.5, value: 'needs-review' }),
      attribute({ propertyId: 'c', confidence: 0.9, value: 'also-critical' }),
    ];
    // 'b' kept its original (critical) value -> still needs review; 'c' was edited -> reviewed.
    const counts = countAttributeReview(attributes, { b: 'needs-review', c: 'edited' });
    expect(counts).toEqual({ total: 3, critical: 2, toReview: 1 });
  });

  it('returns all zeros for an empty attribute list', () => {
    expect(countAttributeReview([], {})).toEqual({ total: 0, critical: 0, toReview: 0 });
  });
});
