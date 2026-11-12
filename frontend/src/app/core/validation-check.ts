import { ExtractedAttribute, ExtractionResult } from './models/extraction.model';
import { CheckAttribute, ValidatedAttribute } from './models/validation.model';

/**
 * The current form values as `/validation/check` input: single values from the form, multi-values
 * as extracted (the form is text only). Empty fields are left out; the DMS hooks may set them, so
 * there is no local mandatory check (ADR-016).
 */
export function buildCheckAttributes(
  edited: Record<string, string>,
  extracted: ExtractedAttribute[],
): CheckAttribute[] {
  const byId = new Map(extracted.map((attribute) => [attribute.propertyId, attribute]));
  const attributes: CheckAttribute[] = [];
  for (const [propertyId, value] of Object.entries(edited)) {
    const original = byId.get(propertyId);
    const origin = original?.origin ?? null;
    if (value.trim() !== '') {
      attributes.push({ propertyId, value, values: null, origin });
    } else if (original?.values?.length) {
      attributes.push({ propertyId, value: null, values: original.values, origin });
    }
  }
  return attributes;
}

/**
 * Takes over what the DMS would store (ADR-016): values and origin into the extraction result,
 * single values also into the form. `changedByDms` and `origin = 'DMS'` stay set on later checks
 * (they say what the DMS did to the suggestion, not what the last call changed). Values the DMS set
 * itself have no confidence and no source. Multi-values are not editable in the form yet, so they
 * only reach the result.
 */
export function applyValidatedAttributes(
  result: ExtractionResult,
  edited: Record<string, string>,
  validated: ValidatedAttribute[],
): { result: ExtractionResult; edited: Record<string, string> } {
  const pending = new Map(validated.map((attribute) => [attribute.propertyId, attribute]));
  const attributes: ExtractedAttribute[] = result.attributes.map((attribute) => {
    const answer = pending.get(attribute.propertyId);
    if (!answer) {
      return attribute;
    }
    pending.delete(attribute.propertyId);
    return answer.origin === 'DMS' || attribute.origin === 'DMS'
      ? fromDms(answer, attribute)
      : {
          ...attribute,
          value: answer.value,
          values: answer.values,
          origin: 'LLM',
          changedByDms: answer.changedByDms || attribute.changedByDms === true,
        };
  });
  for (const answer of pending.values()) {
    attributes.push(fromDms(answer));
  }

  const nextEdited = { ...edited };
  for (const answer of validated) {
    if (answer.value !== null) {
      nextEdited[answer.propertyId] = answer.value;
    }
  }
  return { result: { ...result, attributes }, edited: nextEdited };
}

function fromDms(answer: ValidatedAttribute, previous?: ExtractedAttribute): ExtractedAttribute {
  return {
    propertyId: answer.propertyId,
    value: answer.value,
    values: answer.values,
    confidence: null,
    sourceExcerpt: null,
    origin: 'DMS',
    changedByDms: true,
    correctedAfterValidation: previous?.correctedAfterValidation ?? false,
  };
}
