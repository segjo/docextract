import { fromRows, toRows } from './multi-value-rows';

describe('multi-value rows', () => {
  it('builds one row per position across all properties', () => {
    expect(toRows(['a', 'b'], { a: ['1', '2', '3'], b: ['x'] })).toEqual([
      { a: '1', b: 'x' },
      { a: '2', b: '' },
      { a: '3', b: '' },
    ]);
  });

  it('builds no rows without values', () => {
    expect(toRows(['a'], {})).toEqual([]);
  });

  it('keeps empty cells inside a column and drops trailing ones', () => {
    expect(
      fromRows(
        ['a', 'b'],
        [
          { a: '1', b: '' },
          { a: '', b: 'y' },
          { a: '3', b: ' ' },
        ],
      ),
    ).toEqual({ a: ['1', '', '3'], b: ['', 'y'] });
  });
});
