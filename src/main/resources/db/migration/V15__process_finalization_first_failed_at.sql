-- Start of the promotion retry limit (ADR-015): set on the first failed promotion; after the
-- configured limit the process moves to INDEX_FAILED and loses its TTL-cleanup protection (C-7).
ALTER TABLE process_finalization ADD COLUMN first_failed_at TIMESTAMPTZ;
