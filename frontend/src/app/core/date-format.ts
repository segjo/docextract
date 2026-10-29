import { DmsPropertyDefinition } from './models/retrieval.model';

const SWISS_DATE = /^\s*(\d{1,2})\.(\d{1,2})\.(\d{4})\s*$/;

/** Converts a dd.MM.yyyy value to the yyyy-MM-dd format the DMS expects; other values pass through. */
export function toIsoDate(value: string): string {
  const match = SWISS_DATE.exec(value);
  if (!match) {
    return value;
  }
  const [, day, month, year] = match;
  return `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`;
}

/** Normalizes the values of DATE properties to ISO format before they are sent to the DMS. */
export function normalizeDateAttributes(
  attributes: Record<string, string>,
  definitions: DmsPropertyDefinition[],
): Record<string, string> {
  const dateIds = new Set(
    definitions.filter((definition) => definition.dataType === 'DATE').map((d) => d.id),
  );
  return Object.fromEntries(
    Object.entries(attributes).map(([id, value]) => [
      id,
      dateIds.has(id) ? toIsoDate(value) : value,
    ]),
  );
}
