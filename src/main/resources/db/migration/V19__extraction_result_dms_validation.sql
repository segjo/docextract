-- ADR-016: result of the final DMS check (storevalidate) of an extraction: {"status": "VALID |
-- INVALID | NOT_CHECKED", "error": {...}}. The per-attribute fields origin, changedByDms and
-- correctedAfterValidation live inside the existing `attributes` JSON (one array element per
-- attribute), so they need no column. Rows from before this migration have no validation and no
-- such fields; they are read as NOT_CHECKED and origin = LLM (rows are TTL-bound, no backfill).
ALTER TABLE extraction_result ADD COLUMN validation JSONB;

COMMENT ON COLUMN extraction_result.validation IS
    'ADR-016 final DMS check: status VALID|INVALID|NOT_CHECKED plus the one DMS error; NULL reads as NOT_CHECKED';
COMMENT ON COLUMN extraction_result.attributes IS
    'Attribute array: propertyId, value, values, confidence (0-1, null for origin DMS), sourceExcerpt, origin LLM|DMS, changedByDms, correctedAfterValidation (ADR-016); missing = LLM/false';
