/** One numbered table row of the multi-value dialog: propertyId -> cell text. */
export type MultiValueRow = Record<string, string>;

/** Row `i` holds position `i + 1` of every property (value lists of one row may depend on each other). */
export function toRows(
  propertyIds: readonly string[],
  values: Record<string, readonly string[]>,
): MultiValueRow[] {
  const count = Math.max(0, ...propertyIds.map((id) => values[id]?.length ?? 0));
  return Array.from({ length: count }, (_, index) =>
    Object.fromEntries(propertyIds.map((id) => [id, values[id]?.[index] ?? ''])),
  );
}

/** Empty cells inside a column keep their position; only trailing empty cells are dropped. */
export function fromRows(
  propertyIds: readonly string[],
  rows: readonly MultiValueRow[],
): Record<string, string[]> {
  return Object.fromEntries(
    propertyIds.map((id) => {
      const column = rows.map((row) => row[id] ?? '');
      let end = column.length;
      while (end > 0 && column[end - 1].trim() === '') {
        end--;
      }
      return [id, column.slice(0, end)];
    }),
  );
}
