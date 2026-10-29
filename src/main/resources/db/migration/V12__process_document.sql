-- DMS blob location and original filename on the ORIGINAL blob row, so the validation confirm step
-- can run on any instance and hand them to the DMS write-back (contentLocationUri, filename).
-- They share the blob's TTL cleanup (ADR-008).
ALTER TABLE document_blob
    ADD COLUMN dms_location VARCHAR(2048),
    ADD COLUMN filename     VARCHAR(512);
