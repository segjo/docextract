-- Raw DMS object-properties (FR-3, SPEC §3), fetched live via GET /dms/r/{repositoryId}/o2/{dmsObjectId}/
-- for each similar-documents hit and cached here alongside the score. NULL when repository_id or
-- dms_document_id was blank, or the DMS call failed (best-effort enrichment, not a hard dependency).
ALTER TABLE retrieval_result ADD COLUMN properties JSONB;
