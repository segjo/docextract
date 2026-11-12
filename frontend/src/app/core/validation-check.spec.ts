import { ExtractedAttribute, ExtractionResult } from './models/extraction.model';
import { applyValidatedAttributes, buildCheckAttributes } from './validation-check';

function attribute(partial: Partial<ExtractedAttribute>): ExtractedAttribute {
  return {
    propertyId: 'p1',
    value: 'v',
    values: null,
    confidence: 0.9,
    sourceExcerpt: 'ex',
    ...partial,
  };
}

describe('buildCheckAttributes', () => {
  it('sends non-empty form values and leaves empty fields out (no local mandatory check)', () => {
    const attributes = buildCheckAttributes({ p1: 'x', p2: '', p3: '  ' }, [
      attribute({ propertyId: 'p1' }),
    ]);

    expect(attributes).toEqual([{ propertyId: 'p1', value: 'x', values: null, origin: null }]);
  });

  it('keeps the origin of a value the DMS set earlier', () => {
    const attributes = buildCheckAttributes({ p1: 'Hook' }, [
      attribute({ propertyId: 'p1', origin: 'DMS' }),
    ]);

    expect(attributes[0].origin).toBe('DMS');
  });

  it('sends the extracted multi-values of a field the text form cannot edit', () => {
    const attributes = buildCheckAttributes({ p2: '' }, [
      attribute({ propertyId: 'p2', value: null, values: ['a', 'b'] }),
    ]);

    expect(attributes).toEqual([
      { propertyId: 'p2', value: null, values: ['a', 'b'], origin: null },
    ]);
  });
});

describe('applyValidatedAttributes', () => {
  const result: ExtractionResult = {
    documentTypeId: 't1',
    attributes: [attribute({ propertyId: 'p1', value: '12' }), attribute({ propertyId: 'p2' })],
  };

  it('replaces the value of an LLM attribute and keeps confidence and source', () => {
    const applied = applyValidatedAttributes(result, { p1: '12', p2: 'v' }, [
      { propertyId: 'p1', value: '12.00', values: null, origin: 'LLM', changedByDms: true },
    ]);

    expect(applied.result.attributes[0]).toEqual({
      propertyId: 'p1',
      value: '12.00',
      values: null,
      confidence: 0.9,
      sourceExcerpt: 'ex',
      origin: 'LLM',
      changedByDms: true,
    });
    expect(applied.edited).toEqual({ p1: '12.00', p2: 'v' });
  });

  it('adds a value set by the DMS without confidence and source', () => {
    const applied = applyValidatedAttributes(result, {}, [
      { propertyId: 'p3', value: 'In Prüfung', values: null, origin: 'DMS', changedByDms: true },
    ]);

    expect(applied.result.attributes[2]).toEqual({
      propertyId: 'p3',
      value: 'In Prüfung',
      values: null,
      confidence: null,
      sourceExcerpt: null,
      origin: 'DMS',
      changedByDms: true,
      correctedAfterValidation: false,
    });
    expect(applied.edited).toEqual({ p3: 'In Prüfung' });
  });

  it('turns an empty attribute the DMS filled into a DMS value, in place', () => {
    const emptied: ExtractionResult = {
      ...result,
      attributes: [attribute({ propertyId: 'p1', value: null, confidence: 0 })],
    };

    const applied = applyValidatedAttributes(emptied, { p1: '' }, [
      { propertyId: 'p1', value: 'Hook', values: null, origin: 'DMS', changedByDms: true },
    ]);

    expect(applied.result.attributes).toHaveLength(1);
    expect(applied.result.attributes[0]).toMatchObject({ value: 'Hook', origin: 'DMS' });
  });

  it('keeps a change by the DMS marked when a later check finds the value unchanged', () => {
    const marked: ExtractionResult = {
      ...result,
      attributes: [attribute({ propertyId: 'p1', changedByDms: true })],
    };

    const applied = applyValidatedAttributes(marked, { p1: 'v' }, [
      { propertyId: 'p1', value: 'v', values: null, origin: 'LLM', changedByDms: false },
    ]);

    expect(applied.result.attributes[0].changedByDms).toBe(true);
  });

  it('keeps the correction mark of a field the DMS later set itself', () => {
    const corrected: ExtractionResult = {
      ...result,
      attributes: [attribute({ propertyId: 'p1', correctedAfterValidation: true })],
    };

    const applied = applyValidatedAttributes(corrected, { p1: 'v' }, [
      { propertyId: 'p1', value: 'x', values: null, origin: 'DMS', changedByDms: true },
    ]);

    expect(applied.result.attributes[0].correctedAfterValidation).toBe(true);
  });

  it('leaves attributes the DMS did not report untouched and does not mutate its input', () => {
    const edited = { p1: '12' };
    const applied = applyValidatedAttributes(result, edited, []);

    expect(applied.result.attributes).toEqual(result.attributes);
    expect(applied.edited).toEqual({ p1: '12' });
    expect(applied.edited).not.toBe(edited);
  });

  it('takes multi-values only into the result, not into the text form', () => {
    const applied = applyValidatedAttributes(result, { p2: 'v' }, [
      { propertyId: 'p2', value: null, values: ['a', 'b'], origin: 'LLM', changedByDms: false },
    ]);

    expect(applied.result.attributes[1].values).toEqual(['a', 'b']);
    expect(applied.edited).toEqual({ p2: 'v' });
  });
});
