-- Import repository chosen at upload, on the ORIGINAL blob row like dms_location/filename (V12),
-- so every later step and the confirm request can read it from any instance (ADR-004).
ALTER TABLE document_blob
    ADD COLUMN repository_id VARCHAR(255);
